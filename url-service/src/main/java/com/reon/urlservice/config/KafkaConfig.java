package com.reon.urlservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    // url-service publishes a click event here; analytics-service reads it
    public static final String URL_CLICKED_TOPIC = "url-clicked";

    // created when url-service starts, so we don't depend on Kafka creating topics by itself
    @Bean
    public NewTopic urlClickedTopic() {
        return TopicBuilder
                .name(URL_CLICKED_TOPIC)
                .partitions(4)
                .replicas(1)
                .build();
    }
}
