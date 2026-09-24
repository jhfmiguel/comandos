package com.comandos.core.service;

import com.comandos.core.model.PersonAddress;
import com.comandos.core.model.PersonContactType;
import com.comandos.core.model.PersonEmail;
import com.comandos.core.model.PersonPhone;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class PersonContactTypeBootstrap implements ApplicationRunner {

    private final EntityManager entityManager;

    public PersonContactTypeBootstrap(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensure("RESIDENTIAL", "Residencial", "Tipo residencial.", true, false, false);
        ensure("BUSINESS", "Comercial", "Endereço comercial.", true, false, false);
        ensure("MAILING", "Correspondência", "Endereço para correspondência.", true, false, false);
        ensure("MOBILE", "Celular", "Telefone celular.", false, true, false);
        ensure("WHATSAPP", "WhatsApp", "Contato por WhatsApp.", false, true, false);
        ensure("HOME", "Residencial", "Telefone residencial.", false, true, false);
        ensure("WORK", "Profissional", "Contato profissional.", false, true, true);
        ensure("PERSONAL", "Pessoal", "Contato pessoal.", false, false, true);
        ensure("OTHER", "Outro", "Outro tipo de contato.", true, true, true);

        for (PersonAddress address : entityManager.createQuery(
                "select a from PersonAddress a where a.contactType is null",
                PersonAddress.class).getResultList()) {
            address.contactType = ensureLegacy(address.type, true, false, false);
        }

        for (PersonPhone phone : entityManager.createQuery(
                "select p from PersonPhone p where p.contactType is null",
                PersonPhone.class).getResultList()) {
            phone.contactType = ensureLegacy(phone.type, false, true, false);
        }

        for (PersonEmail email : entityManager.createQuery(
                "select e from PersonEmail e where e.contactType is null",
                PersonEmail.class).getResultList()) {
            email.contactType = ensureLegacy(email.type, false, false, true);
        }
    }

    private PersonContactType ensureLegacy(
            String rawCode,
            boolean addressEnabled,
            boolean phoneEnabled,
            boolean emailEnabled) {

        String raw = rawCode == null ? "" : rawCode.trim();
        String code = raw.isBlank()
            ? "OTHER"
            : raw.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return ensure(
            code.isBlank() ? "OTHER" : code,
            raw.isBlank() ? "Outro" : raw,
            "Migrado automaticamente do tipo de contato legado.",
            addressEnabled,
            phoneEnabled,
            emailEnabled
        );
    }

    private PersonContactType ensure(
            String code,
            String name,
            String description,
            boolean addressEnabled,
            boolean phoneEnabled,
            boolean emailEnabled) {

        var existing = entityManager.createQuery(
                "select t from PersonContactType t where upper(t.code) = :code",
                PersonContactType.class)
            .setParameter("code", code.toUpperCase(Locale.ROOT))
            .setMaxResults(1)
            .getResultList();

        PersonContactType value;
        if (!existing.isEmpty()) {
            value = existing.getFirst();
        } else {
            value = new PersonContactType();
            value.code = code.toUpperCase(Locale.ROOT);
            value.name = name;
            value.description = description;
            value.active = true;
            entityManager.persist(value);
        }

        value.addressEnabled = Boolean.TRUE.equals(value.addressEnabled) || addressEnabled;
        value.phoneEnabled = Boolean.TRUE.equals(value.phoneEnabled) || phoneEnabled;
        value.emailEnabled = Boolean.TRUE.equals(value.emailEnabled) || emailEnabled;
        return value;
    }
}
