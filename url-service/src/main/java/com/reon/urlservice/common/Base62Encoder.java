package com.reon.urlservice.common;

public class Base62Encoder {
    private static final String alphabets = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int Base = alphabets.length();
    private static final int target_length = 6;

    private Base62Encoder(){}


    public static String encode(Long id){
        if (id <= 0) {
            throw new IllegalArgumentException("Id must be a positive number, provide Id: {}" +  id);
        }

        StringBuilder shortCode = new StringBuilder();
        long value = id;


        while (value > 0) {
            shortCode.append(alphabets.charAt((int) (value % Base)));
            value /= Base;
        }

        while (shortCode.length() < target_length) {
            shortCode.append('a');
        }

        return shortCode.reverse().toString();
    }
}

