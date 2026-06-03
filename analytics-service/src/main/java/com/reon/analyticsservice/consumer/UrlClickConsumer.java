package com.reon.analyticsservice.consumer;

import com.reon.analyticsservice.document.Analytics;
import com.reon.analyticsservice.repository.AnalyticsRepository;
import com.reon.events.UrlClickEvent;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UrlClickConsumer {
    private final Logger log = LoggerFactory.getLogger(UrlClickConsumer.class);
    private final AnalyticsRepository analyticsRepository;
    private final UserAgentAnalyzer uaa;

    public UrlClickConsumer(AnalyticsRepository analyticsRepository, UserAgentAnalyzer uaa) {
        this.analyticsRepository = analyticsRepository;
        this.uaa = uaa;
    }

    @KafkaListener(topics = "url-clicked", groupId = "analytics-service-group")
    public void consume(UrlClickEvent event) {
        log.info("UrlClickConsumer :: Consumed event for shortCode: {}", event.shortCode());

        UserAgent parsedUa = uaa.parse(event.userAgent());

        Analytics analytics = Analytics.builder()
                .shortCode(event.shortCode())
                .userId(event.userId())
                .urlId(event.urlId())
                .ipAddress(event.ipAddress())
                .referrer(event.referrer())
                .clickedAt(event.clickedAt())
                .browser(parsedUa.getValue(UserAgent.AGENT_NAME))
                .os(parsedUa.getValue(UserAgent.OPERATING_SYSTEM_NAME_VERSION))
                .deviceType(parsedUa.getValue(UserAgent.DEVICE_CLASS))
                .country("Unknown") // TODO: Implement GeoIP
                .city("Unknown")
                .build();

        analyticsRepository.save(analytics);
        log.info("UrlClickConsumer :: Saved analytics for shortCode: {}", event.shortCode());
    }
}
