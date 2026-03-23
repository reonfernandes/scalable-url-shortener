package com.reon.exception;

public class PasswordRequiredException extends RuntimeException{
    public PasswordRequiredException() {
        super("URL is password protected");
    }
}
