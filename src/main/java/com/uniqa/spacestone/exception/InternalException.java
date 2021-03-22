package com.uniqa.spacestone.exception;

public class InternalException extends RuntimeException {
    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Exception e) {
        super(String.format(message, String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage())));
    }

    public InternalException(String message, Object... args) {
        super(String.format(message, args));
    }
}
