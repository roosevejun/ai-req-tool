package com.tongtu.docgen.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.tongtu.docgen.system.exception.ForbiddenException;
import com.tongtu.docgen.system.exception.UnauthorizedException;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Object> handleBadRequest(IllegalArgumentException e) {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.error(10001, e.getMessage(), traceId);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleAny(Exception e) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception, traceId={}", traceId, e);
        return ApiResponse.error(20001, "Internal server error", traceId);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse<Object> handleUnauthorized(UnauthorizedException e) {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.error(40101, e.getMessage(), traceId);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse<Object> handleForbidden(ForbiddenException e) {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.error(40301, e.getMessage(), traceId);
    }
}

