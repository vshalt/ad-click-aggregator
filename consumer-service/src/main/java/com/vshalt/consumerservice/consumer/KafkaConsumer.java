package com.vshalt.consumerservice.consumer;

import com.vshalt.consumerservice.model.LogEntity;
import com.vshalt.consumerservice.repository.LogRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
public class KafkaConsumer {
    @Autowired
    private LogRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private final MeterRegistry registry;
    private final Counter eventsProcessed;
    private final Timer batchDuration;

    public KafkaConsumer(MeterRegistry registry) {
        this.registry = registry;
        this.eventsProcessed = Counter.builder("consumer.events.processed").register(registry);
        this.batchDuration = Timer.builder("consumer.batch.duration")
                .publishPercentileHistogram()
                .register(registry);
    }

    @KafkaListener(
            topics="${app.kafka.ingestion-topic-name}",
            groupId="${app.kafka.ingestion-group-id}",
            concurrency="${app.kafka.ingestion-concurrency}"
    )
    @Transactional
    public void consume(List<String> payload, Acknowledgment ack) {
        Timer.Sample sample = Timer.start(registry);
        List<LogEntity> entities = new ArrayList<>();
        for (int i=0; i<payload.size(); i++) {
            try {
                JsonNode node = objectMapper.readTree(payload.get(i));
                LogEntity entity = new LogEntity();
                entity.setService(node.path("service").asString());
                entity.setLevel(node.path("level").asString());
                entity.setMessage(node.path("message").asString());
                entity.setTime(Instant.parse(node.path("timestamp").asString()));
                entity.setUuid(node.path("uuid").asString());
                entities.add(entity);
            } catch (Exception e) {
                throw new BatchListenerFailedException("Failed to parse", e, i);
            }
        }
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
        }
        eventsProcessed.increment(entities.size());
        sample.stop(batchDuration);
        if (ack != null) {
            ack.acknowledge();
        }
    }
}
