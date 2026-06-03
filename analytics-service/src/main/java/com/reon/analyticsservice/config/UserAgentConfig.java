package com.reon.analyticsservice.config;

import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentConfig {

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .withCache(10000)
                .build();
    }
}
