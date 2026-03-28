package com.reon.userservice.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OTPGenerator {
    private static final SecureRandom random = new SecureRandom();
    private static final int Otp_length = 6;

    private OTPGenerator() {}

    public static String generateOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
