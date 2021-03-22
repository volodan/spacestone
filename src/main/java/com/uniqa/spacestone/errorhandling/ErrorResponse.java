package com.uniqa.spacestone.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class ErrorResponse {
    private String code;
    private String message;

    public ErrorResponse() {
    }

    public static ErrorResponse from(HttpStatusCodeException e) {
        return from(e.getStatusCode(), e.getMessage());
    }

    public static ErrorResponse from(HttpStatus httpStatus, String message) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(httpStatus.toString());
        errorResponse.setMessage(message);
        return errorResponse;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

