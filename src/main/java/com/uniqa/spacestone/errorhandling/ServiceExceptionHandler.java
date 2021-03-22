package com.uniqa.spacestone.errorhandling;

import com.uniqa.spacestone.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Order(11)
class ServiceExceptionHandler {
    private final ErrorLogUtil errorLogUtil;

    @Autowired
    public ServiceExceptionHandler(ErrorLogUtil errorLogUtil) {
        this.errorLogUtil = errorLogUtil;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({BadRequestException.class})
    @ResponseBody
    public ErrorResponse handle(BadRequestException e) {
        return this.errorLogUtil.logAndCreateErrorRespone(HttpStatus.BAD_REQUEST, e);
    }
}
