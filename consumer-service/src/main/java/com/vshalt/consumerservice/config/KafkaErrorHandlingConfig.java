package com.vshalt.consumerservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlingConfig {
    @Value("${app.kafka.ingestion-dlt-topic-name}")
    private String dltTopic;

    @Value ("${app.kafka.ingestion-dlt-topic-partitions}")
    private int partitions;

    @Value ("${app.kafka.ingestion-dlt-topic-replicas}")
    private int replicas;

    @Bean
    public NewTopic ingestionDlt() {
        return TopicBuilder.name(dltTopic).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, exception) -> new TopicPartition(dltTopic, -1)
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
