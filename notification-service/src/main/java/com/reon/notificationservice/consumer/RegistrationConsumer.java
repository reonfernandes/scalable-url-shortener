package com.reon.notificationservice.consumer;

import com.reon.events.RegistrationSuccessEvent;
import com.reon.notificationservice.service.RegistrationMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationConsumer {

    private final Logger log = LoggerFactory.getLogger(RegistrationConsumer.class);
    private final RegistrationMailService registrationMailService;

    public RegistrationConsumer(RegistrationMailService registrationMailService) {
        this.registrationMailService = registrationMailService;
    }

    @KafkaListener(
            topics = "user.registered",
            groupId = "notify-group"
    )
    public void consumeRegistrationEvent(RegistrationSuccessEvent registrationSuccessEvent) {
        log.info("Notification Service:: Consuming Registration Success Event");
        registrationMailService.sendMailToNewlyRegisteredUser(registrationSuccessEvent);
    }
}
