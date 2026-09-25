package com.reon.urlservice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

public class Base62Encoder {
    private final static Logger log = LoggerFactory.getLogger(Base62Encoder.class);
    private static final String alphabets = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int Base = alphabets.length();
    private static final int target_length = 6;
    private static final SecureRandom random = new SecureRandom();

    private Base62Encoder(){}


    public static String encode(Long id){
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be a positive number, provided Id: " + id);
        }

        log.info("Base62 Encoder :: Generating a new short code");

        StringBuilder shortCode = new StringBuilder();
        long value = id;

        while (value > 0) {
            shortCode.append(alphabets.charAt((int) (value % Base)));
            value /= Base;
        }

        while (shortCode.length() < target_length) {
            shortCode.append('a');
        }

        log.info("Base62 Encoder :: Short code generated");

        return shortCode.reverse().toString();
    }

    /** A random code of the given length, used when the code for an id is already taken. */
    public static String random(int length) {
        StringBuilder shortCode = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            shortCode.append(alphabets.charAt(random.nextInt(Base)));
        }
        return shortCode.toString();
    }
}
