package com.reon.exception;

public class InvalidUrlPasswordException extends RuntimeException{
    public InvalidUrlPasswordException() {
        super("Incorrect password");
    }
}
