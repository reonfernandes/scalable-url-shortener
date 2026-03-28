package com.reon.notificationservice.service.impl;

import com.reon.events.RegistrationSuccessEvent;
import com.reon.notificationservice.service.RegistrationMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegistrationMailServiceImpl implements RegistrationMailService {
    private final String mailSender;

    private final Logger log = LoggerFactory.getLogger(RegistrationMailServiceImpl.class);
    private final JavaMailSender javaMailSender;

    public RegistrationMailServiceImpl(@Value("${security.email.sender}") String mailSender, JavaMailSender javaMailSender) {
        this.mailSender = mailSender;
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendMailToNewlyRegisteredUser(RegistrationSuccessEvent registrationSuccessEvent) {
        log.info("Notification Service :: Sending otp verification mail to user: {}", registrationSuccessEvent.userId());

        SimpleMailMessage mail = new SimpleMailMessage();

        String mailMessage = "Hi, " + registrationSuccessEvent.name() + "\n\n"
                + "Thank you for registering with us.\n\n"
                + "To complete your registration, please use the One-Time Password (OTP) below:\n\n"
                + "OTP: " + registrationSuccessEvent.otp() + "\n\n"
                + "This OTP is valid for the next 30 minutes. Please do not share this code with anyone.\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "Regards,\nShortly Team";

        mail.setSubject("Verify Your Account – OTP Code");
        mail.setFrom(mailSender);
        mail.setTo(registrationSuccessEvent.email());
        mail.setText(mailMessage);

        javaMailSender.send(mail);
        log.info("Notification Service :: Otp verification email sent successfully.");
    }
}
