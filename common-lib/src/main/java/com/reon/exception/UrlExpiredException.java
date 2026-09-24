package com.reon.exception;

public class UrlExpiredException extends RuntimeException{
    public UrlExpiredException() {
        super("URL has expired");
    }
}
