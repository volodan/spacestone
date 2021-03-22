package com.uniqa.spacestone.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Object... args) {
        super(String.format(message, args));
    }
}
