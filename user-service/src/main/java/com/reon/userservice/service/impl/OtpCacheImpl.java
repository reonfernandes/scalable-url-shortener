package com.reon.userservice.service.impl;

import com.reon.userservice.service.OtpCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OtpCacheImpl implements OtpCache {
    private static final String OTP_PREFIX = "otp_";
    private final RedisTemplate<String, String> redisTemplate;

    public OtpCacheImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeOtp(String otp, String email, Long duration) {
        redisTemplate.opsForValue()
                .set(buildOtpKey(email), otp, duration, TimeUnit.MINUTES);
    }

    @Override
    public String getOtp(String email) {
        return redisTemplate.opsForValue().get(buildOtpKey(email));
    }

    @Override
    public void deleteOtp(String email) {
        redisTemplate.delete(buildOtpKey(email));
    }

    private String buildOtpKey(String email) {
        return OTP_PREFIX + email;
    }
}
