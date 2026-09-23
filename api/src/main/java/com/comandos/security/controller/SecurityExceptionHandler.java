package com.comandos.security.controller;

import com.comandos.core.web.ApiProblems;
import com.comandos.security.api.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail denied(AccessDeniedException exception) {
        return ApiProblems.of(
            HttpStatus.FORBIDDEN,
            exception.getMessage(),
            "You do not have permission for this operation or scope."
        );
    }
}
