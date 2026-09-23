package com.comandos.core.controller;

import com.comandos.core.web.ApiProblems;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = CoreController.class)
public class CoreExceptionHandler {
	
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail invalid(ResponseStatusException ex) {
        
        return ApiProblems.of(
            ex.getStatusCode(),
            ex.getReason(),
            "Invalid request."
        );
    
    }

    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    public ProblemDetail integrity(Exception ex) {
        
        return ApiProblems.integrityConflict();
    
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail concurrentEdit(Exception ex) {
        
        return ApiProblems.concurrentEdit();
    	
    }
    
}
