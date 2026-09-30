package com.comandos.core.service;

import com.fariamiguel.core.api.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidGenerator implements IdGenerator {
    @Override
    public UUID next() {
        return UUID.randomUUID();
    }
}
