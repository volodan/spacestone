package com.uniqa.spacestone.errorhandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class ErrorLogUtil {
    private static final Logger log = LoggerFactory.getLogger(ErrorLogUtil.class);

    public ErrorResponse logAndCreateErrorRespone(HttpStatus httpStatus, Exception e) {
        return this.logAndCreateErrorRespone("An exception occurred: {} with message: {}", httpStatus, e);
    }

    private ErrorResponse logAndCreateErrorRespone(String logMessage, HttpStatus httpStatus, Exception e) {
        log.error(logMessage, new Object[]{e.getClass().getSimpleName(), e.getMessage()});
        return ErrorResponse.from(httpStatus, httpStatus.is5xxServerError() ? "An internal error occurred" : "Please check your request");
    }
}
