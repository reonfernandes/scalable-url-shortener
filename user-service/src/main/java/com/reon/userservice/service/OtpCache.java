package com.reon.userservice.service;

public interface OtpCache {
    void storeOtp(String otp, String email, Long duration);
    String getOtp(String email);
    void deleteOtp(String email);
}
