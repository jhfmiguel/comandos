package com.comandos.lifecycle.web;

import com.fariamiguel.app.workflow.controller.WorkflowController;
import com.fariamiguel.app.lifecycle.controller.LifecycleController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
    WorkflowController.class,
    LifecycleController.class
})
public class WorkflowLifecycleProblemHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail businessError(ResponseStatusException exception) {
        String detail = exception.getReason();

        if (detail == null || detail.isBlank()) {
            detail = "The requested operation cannot be completed.";
        }

        return ProblemDetail.forStatusAndDetail(
            exception.getStatusCode(),
            detail
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidRequest(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid or unreadable request body."
        );
    }
}