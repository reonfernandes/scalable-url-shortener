package com.reon.userservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    private final String registerSuccessTopic;
    private final String resendVerificationOtp;
    private final String userAccountDeleteTopic;

    public KafkaConfig(@Value("${security.kafka.topic.register}") String registerSuccessTopic,
                       @Value("${security.kafka.topic.resend}") String resendVerificationOtp,
                       @Value("${security.kafka.topic.deleted}") String userAccountDeleteTopic) {
        this.registerSuccessTopic = registerSuccessTopic;
        this.resendVerificationOtp = resendVerificationOtp;
        this.userAccountDeleteTopic = userAccountDeleteTopic;
    }

    @Bean
    public NewTopic registrationSuccessTopic() {
        return TopicBuilder
                .name(registerSuccessTopic)
                .partitions(4)
                .replicas(1)
                .build();
    }

    // TODO:: Pending yet to implement
    @Bean
    public NewTopic resendVerificationOtpTopic() {
        return TopicBuilder
                .name(resendVerificationOtp)
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder
                .name(userAccountDeleteTopic)
                .partitions(4)
                .replicas(1)
                .build();
    }
}
