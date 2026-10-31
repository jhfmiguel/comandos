package com.comandos.core.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates person creation with addresses, phones and e-mails while write
 * persistence is still handled by the legacy CoreService.
 *
 * <p>The orchestration is isolated so the monolithic CoreService can shrink
 * before the final canonical persistence cutover.</p>
 */
@Service
public class PersonRegistrationService {

    public record PersonRegistration(
        Map<String, Object> person,
        List<Map<String, Object>> addresses,
        List<Map<String, Object>> phones,
        List<Map<String, Object>> emails
    ) {}

    private final CoreService core;

    public PersonRegistrationService(CoreService core) {
        this.core = core;
    }

    @Transactional
    public Map<String, Object> save(PersonRegistration registration) {
        if (registration == null || registration.person() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Person data is required."
            );
        }

        Map<String, Object> person =
            core.save("people", null, registration.person());

        long personId = ((Number) person.get("id")).longValue();

        int addressCount = saveContacts(
            "person-addresses",
            personId,
            registration.addresses()
        );
        int phoneCount = saveContacts(
            "person-phones",
            personId,
            registration.phones()
        );
        int emailCount = saveContacts(
            "person-emails",
            personId,
            registration.emails()
        );

        Map<String, Object> result = new LinkedHashMap<>(person);
        result.put("addressesCreated", addressCount);
        result.put("phonesCreated", phoneCount);
        result.put("emailsCreated", emailCount);
        return result;
    }

    private int saveContacts(
            String resource,
            long personId,
            List<Map<String, Object>> contacts) {

        if (contacts == null || contacts.isEmpty()) return 0;

        int created = 0;
        for (Map<String, Object> contact : contacts) {
            if (contact == null) continue;

            Map<String, Object> data = new LinkedHashMap<>(contact);
            data.put("personId", personId);
            data.remove("id");
            data.remove("version");
            core.save(resource, null, data);
            created++;
        }
        return created;
    }
}
