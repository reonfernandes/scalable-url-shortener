package com.reon.notificationservice.service;

import com.reon.events.RegistrationSuccessEvent;

public interface RegistrationMailService {
    void sendMailToNewlyRegisteredUser(RegistrationSuccessEvent registrationSuccessEvent);
}
