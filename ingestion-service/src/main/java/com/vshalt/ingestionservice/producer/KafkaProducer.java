package com.vshalt.ingestionservice.producer;

import com.vshalt.ingestionservice.dto.request.Log;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);

    @Autowired
    private KafkaTemplate<String, Log> template;

    @Value("${app.kafka.ingestion-topic-name}")
    private String topic;

    @Value("${app.kafka.ingestion-topic-partitions}")
    private int partitions;

    @Value("${app.kafka.ingestion-topic-replicas}")
    private int replicas;

    @Bean
    public NewTopic ingestion() {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }

    public void sendEvent(Log logEvent) {
        template.send(topic, logEvent).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic '{}'", topic, ex);
            }
        });
    }
}