package com.padaks.todaktodak.common.exception;

import com.padaks.todaktodak.common.exception.exceptionType.ExceptionType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ExceptionType exceptionType = e.getExceptionType();
        e.printStackTrace();
        return ResponseEntity.status(exceptionType.httpStatus())
                .body(ErrorResponse.of(exceptionType));
    }
}