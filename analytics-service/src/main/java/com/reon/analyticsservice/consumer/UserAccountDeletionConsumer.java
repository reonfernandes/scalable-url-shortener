package com.reon.analyticsservice.consumer;

import com.reon.analyticsservice.repository.AnalyticsRepository;
import com.reon.events.UserAccountDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDeletionConsumer {
    private final Logger log = LoggerFactory.getLogger(UserAccountDeletionConsumer.class);
    private final AnalyticsRepository analyticsRepository;

    public UserAccountDeletionConsumer(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    // the user's links are deleted by url-service; their click data is deleted here
    @KafkaListener(topics = "user.deleted", groupId = "analytics-service-group")
    public void consumeAccountDeletionEvent(UserAccountDeletedEvent event) {
        log.info("Analytics Service :: Deleting click data for deleted user: {}", event.userId());
        long deleted = analyticsRepository.deleteByUserId(event.userId());
        log.info("Analytics Service :: Deleted {} clicks for user: {}", deleted, event.userId());
    }
}
