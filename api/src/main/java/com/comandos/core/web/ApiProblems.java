package com.comandos.core.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public final class ApiProblems {

    private ApiProblems() {
    }

    public static ProblemDetail of(
        HttpStatusCode status,
        String detail,
        String fallback
    ) {
        String message = detail == null || detail.isBlank()
            ? fallback
            : detail;

        return ProblemDetail.forStatusAndDetail(status, message);
    }

    public static ProblemDetail badRequest(String detail) {
        return of(
            HttpStatus.BAD_REQUEST,
            detail,
            "Invalid request."
        );
    }

    public static ProblemDetail notFound(String detail) {
        return of(
            HttpStatus.NOT_FOUND,
            detail,
            "Record not found."
        );
    }

    public static ProblemDetail conflict(String detail) {
        return of(
            HttpStatus.CONFLICT,
            detail,
            "The request conflicts with the current state."
        );
    }

    public static ProblemDetail integrityConflict() {
        return conflict(
            "Cannot save or delete: a duplicate record or a reference from another record exists."
        );
    }

    public static ProblemDetail concurrentEdit() {
        return conflict(
            "The record was changed by another operation. Reload and try again."
        );
    }
}
