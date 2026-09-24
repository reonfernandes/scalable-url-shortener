package com.reon.urlservice.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Base62Encoder {
    private final static Logger log = LoggerFactory.getLogger(Base62Encoder.class);
    private static final String alphabets = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int Base = alphabets.length();
    private static final int target_length = 6;

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
}

