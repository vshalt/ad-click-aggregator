package com.vshalt.parquetservice.consumer;

import com.vshalt.parquetservice.model.LogRecord;
import com.vshalt.parquetservice.writer.ParquetBatchWriter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: handle duplicates
@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final ParquetBatchWriter writer;
    private final KafkaTemplate<Object, Object> template;
    private final String dltTopic;

    private final MeterRegistry registry;
    private final Counter eventsWritten;
    private final Counter eventsRejected;
    private final Counter filesWritten;
    private final Timer batchDuration;

    public KafkaConsumer(ObjectMapper objectMapper,
                         ParquetBatchWriter writer,
                         KafkaTemplate<Object, Object> template,
                         MeterRegistry registry,
                         @Value("${app.kafka.ingestion-dlt-topic-name}") String dltTopic) {
        this.objectMapper = objectMapper;
        this.writer = writer;
        this.template = template;
        this.dltTopic = dltTopic;
        this.registry = registry;
        this.eventsWritten = Counter.builder("parquet.events.written").register(registry);
        this.eventsRejected = Counter.builder("parquet.events.rejected").register(registry);
        this.filesWritten = Counter.builder("parquet.files.written").register(registry);
        this.batchDuration = Timer.builder("parquet.batch.duration")
                .publishPercentileHistogram()
                .register(registry);
    }

    @KafkaListener(
            topics = "${app.kafka.ingestion-topic-name}",
            groupId = "${app.kafka.parquet-group-id}",
            concurrency = "${app.kafka.parquet-concurrency}"
    )
    public void consume(List<String> payload, Acknowledgment ack) throws SQLException, IOException {
        Timer.Sample sample = Timer.start(registry);

        List<LogRecord> records = new ArrayList<>(payload.size());
        List<String> rejected = new ArrayList<>();
        for (String raw : payload) {
            try {
                records.add(parse(raw));
            } catch (Exception e) {
                log.warn("Skipping unparseable message: {}", e.toString());
                rejected.add(raw);
            }
        }

        int files = writer.write(records).size();
        parkRejected(rejected);

        eventsWritten.increment(records.size());
        eventsRejected.increment(rejected.size());
        filesWritten.increment(files);
        sample.stop(batchDuration);
        if (ack != null) {
            ack.acknowledge();
        }
    }

    private LogRecord parse(String raw) {
        JsonNode node = objectMapper.readTree(raw);
        return new LogRecord(
                node.path("uuid").asString(),
                Instant.parse(node.path("timestamp").asString()),
                node.path("service").asString(),
                node.path("level").asString(),
                node.path("message").asString()
        );
    }

    private void parkRejected(List<String> rejected) {
        try {
            for (String raw : rejected) {
                template.send(dltTopic, raw).get(30, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to DLT", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to publish rejected message to " + dltTopic, e);
        }
    }
}
