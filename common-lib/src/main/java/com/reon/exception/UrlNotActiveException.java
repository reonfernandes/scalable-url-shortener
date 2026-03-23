package com.reon.exception;

public class UrlNotActiveException extends RuntimeException{
    public UrlNotActiveException() {
        super("This url is no longer active.");
    }
}
