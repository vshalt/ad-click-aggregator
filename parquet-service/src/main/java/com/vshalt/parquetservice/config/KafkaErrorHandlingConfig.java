package com.vshalt.parquetservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorHandlingConfig {
    @Value("${app.kafka.ingestion-dlt-topic-name}")
    private String dltTopic;

    @Value("${app.kafka.ingestion-dlt-topic-partitions}")
    private int partitions;

    @Value("${app.kafka.ingestion-dlt-topic-replicas}")
    private int replicas;

    @Bean
    public NewTopic parquetDlt() {
        return TopicBuilder.name(dltTopic).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public DefaultErrorHandler errorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(30_000L);
        return new DefaultErrorHandler(backOff);
    }
}
