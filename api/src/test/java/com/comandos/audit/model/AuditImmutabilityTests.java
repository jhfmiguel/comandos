package com.comandos.audit.model;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditImmutabilityTests {
    @Test
    void auditRecordCannotBeDeleted() throws Exception {
        var method = AuditRecord.class.getDeclaredMethod("preventRemoval");
        method.setAccessible(true);

        var exception = assertThrows(
            InvocationTargetException.class,
            () -> method.invoke(new AuditRecord())
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(
            "Audit records cannot be deleted.",
            exception.getCause().getMessage()
        );
    }

    @Test
    void auditReferenceCannotBeDeleted() throws Exception {
        var method = AuditReference.class.getDeclaredMethod("preventRemoval");
        method.setAccessible(true);

        var exception = assertThrows(
            InvocationTargetException.class,
            () -> method.invoke(new AuditReference())
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(
            "Audit references cannot be deleted.",
            exception.getCause().getMessage()
        );
    }
}
