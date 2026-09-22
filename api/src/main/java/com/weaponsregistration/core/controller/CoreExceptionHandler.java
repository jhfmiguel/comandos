package com.weaponsregistration.core.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = CoreController.class)
public class CoreExceptionHandler {
	
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail invalid(ResponseStatusException ex) {
        
    	return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason() == null ? "Invalid request." : ex.getReason());
    
    }

    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    public ProblemDetail integrity(Exception ex) {
        
    	return ProblemDetail
    			.forStatusAndDetail(HttpStatus.CONFLICT, "Cannot save or delete: a duplicate record or a reference from another record exists.");
    
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail concurrentEdit(Exception ex) {
        
    	return ProblemDetail
    			.forStatusAndDetail(HttpStatus.CONFLICT, "The record was changed by another operation. Reload and try again.");
    	
    }
    
}
