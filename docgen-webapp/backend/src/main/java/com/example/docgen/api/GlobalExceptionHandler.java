package com.example.docgen.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Object> handleBadRequest(IllegalArgumentException e) {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.error(10001, e.getMessage(), traceId);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleAny(Exception e) {
        String traceId = UUID.randomUUID().toString();
        // For safety, don't leak internal details to client; server logs can include stack traces.
        return ApiResponse.error(20001, "内部错误", traceId);
    }
}

