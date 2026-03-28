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

    public KafkaConfig(@Value("${security.kafka.topic.register}") String registerSuccessTopic,
                       @Value("${security.kafka.topic.resend}") String resendVerificationOtp) {
        this.registerSuccessTopic = registerSuccessTopic;
        this.resendVerificationOtp = resendVerificationOtp;
    }

    @Bean
    public NewTopic registrationSuccessTopic() {
        return TopicBuilder
                .name(registerSuccessTopic)
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic resendVerificationOtpTopic() {
        return TopicBuilder
                .name(resendVerificationOtp)
                .partitions(4)
                .replicas(1)
                .build();
    }
}
