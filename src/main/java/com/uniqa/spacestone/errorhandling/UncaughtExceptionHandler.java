package com.uniqa.spacestone.errorhandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class UncaughtExceptionHandler {
    private final ErrorLogUtil errorLogUtil;

    @Autowired
    public UncaughtExceptionHandler(ErrorLogUtil errorLogUtil) {
        this.errorLogUtil = errorLogUtil;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public ErrorResponse handle(Exception e) {
        return this.errorLogUtil.logAndCreateErrorRespone(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
