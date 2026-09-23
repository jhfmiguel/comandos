package com.comandos.core.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiProblemsTests {
    @Test
    void usesProvidedDetailAndFallbackWhenBlank() {
        var provided = ApiProblems.of(HttpStatus.BAD_REQUEST, "Invalid field.", "Fallback.");
        assertEquals(400, provided.getStatus());
        assertEquals("Invalid field.", provided.getDetail());

        var fallback = ApiProblems.of(HttpStatus.NOT_FOUND, "   ", "Record not found.");
        assertEquals(404, fallback.getStatus());
        assertEquals("Record not found.", fallback.getDetail());
    }

    @Test
    void exposesCanonicalConflictProblems() {
        var integrity = ApiProblems.integrityConflict();
        assertEquals(409, integrity.getStatus());
        assertEquals(
            "Cannot save or delete: a duplicate record or a reference from another record exists.",
            integrity.getDetail()
        );

        var concurrent = ApiProblems.concurrentEdit();
        assertEquals(409, concurrent.getStatus());
        assertEquals(
            "The record was changed by another operation. Reload and try again.",
            concurrent.getDetail()
        );
    }
}
