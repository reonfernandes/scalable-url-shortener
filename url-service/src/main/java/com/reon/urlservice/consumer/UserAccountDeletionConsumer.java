package com.reon.urlservice.consumer;

import com.reon.events.UserAccountDeletedEvent;
import com.reon.urlservice.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserAccountDeletionConsumer {
    private final Logger log = LoggerFactory.getLogger(UserAccountDeletionConsumer.class);
    private final UrlService urlService;

    public UserAccountDeletionConsumer(UrlService urlService) {
        this.urlService = urlService;
    }

    @KafkaListener(
            topics = "user.deleted",
            groupId = "url-service-group"
    )
    public void consumeAccountDeletionEvent(UserAccountDeletedEvent userAccountDeletedEvent) {
        log.info("URL Service :: Consuming User Account Deletion Event");
        urlService.deleteUserUrls(userAccountDeletedEvent.userId());
        log.info("URL Service :: Event consumed");
    }
}
