package com.reon.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private final String userAccountDeleteTopic;

    private final String adminStateTopic;


    public KafkaConfig(@Value("${security.kafka.topic.deleted}") String userAccountDeleteTopic,
                       @Value("${security.kafka.topic.admin.userState}") String adminStateTopic) {
        this.userAccountDeleteTopic = userAccountDeleteTopic;
        this.adminStateTopic = adminStateTopic;
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder
                .name(userAccountDeleteTopic)
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic adminUserState() {
        return TopicBuilder
                .name(adminStateTopic)
                .partitions(4)
                .replicas(1)
                .build();
    }
}
