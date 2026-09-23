package com.comandos.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreExceptionHandlerTests {
    private final CoreExceptionHandler handler = new CoreExceptionHandler();

    @Test
    void mapsResponseStatusExceptionToProblemDetail() {
        var problem = handler.invalid(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value."));

        assertEquals(400, problem.getStatus());
        assertEquals("Invalid value.", problem.getDetail());
    }

    @Test
    void mapsIntegrityAndOptimisticLockConflicts() {
        var integrity = handler.integrity(new DataIntegrityViolationException("duplicate"));
        assertEquals(409, integrity.getStatus());

        var concurrent = handler.concurrentEdit(
            new ObjectOptimisticLockingFailureException("Record", 1L)
        );
        assertEquals(409, concurrent.getStatus());
    }
}
