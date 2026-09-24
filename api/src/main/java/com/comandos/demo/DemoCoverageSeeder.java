package com.comandos.demo;

import jakarta.persistence.*;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Complements the curated demo data with one synthetic row for every empty
 * application entity table that can be safely instantiated from JPA metadata.
 *
 * Runs only when demo seeding is enabled and executes after curated scenario seeders. Each entity is seeded in its own transaction,
 * so one unsupported relationship never rolls back the other demo records.
 */
@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1000)
public class DemoCoverageSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCoverageSeeder.class);

    private final EntityManager entityManager;
    private final TransactionTemplate transactions;
    private final AtomicLong sequence = new AtomicLong(1000);

    @Value("${comandos.demo.require-full-coverage:false}")
    private boolean requireFullCoverage;

    public DemoCoverageSeeder(EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<Class<?>> pending = new LinkedHashSet<>();

        entityManager.getMetamodel().getEntities().stream()
            .map(EntityType::getJavaType)
            .filter(type -> type.getPackageName().startsWith("com.comandos"))
            .filter(type -> !Modifier.isAbstract(type.getModifiers()))
            .sorted(Comparator.comparing(Class::getName))
            .forEach(pending::add);

        for (int pass = 0; pass < 6 && !pending.isEmpty(); pass++) {
            boolean progressed = false;

            for (Class<?> type : new ArrayList<>(pending)) {
                boolean seeded = seedInOwnTransaction(type);
                if (seeded) {
                    pending.remove(type);
                    progressed = true;
                }
            }

            if (!progressed) break;
        }

        if (pending.isEmpty()) {
            log.info("Demo coverage seeder populated every mapped application entity table.");
        } else {
            var missing = pending.stream().map(Class::getSimpleName).sorted().toList();
            String message = "Demo coverage seeder could not auto-populate "
                + pending.size() + " mapped tables: " + missing;
            if (requireFullCoverage) {
                throw new IllegalStateException(message);
            }
            log.warn(message);
        }
    }

    private boolean seedInOwnTransaction(Class<?> type) {
        try {
            Boolean result = transactions.execute(status -> {
                try {
                    if (count(type) > 0) return true;

                    Object entity = instantiate(type, new LinkedHashSet<>());
                    if (entity == null) return false;

                    entityManager.persist(entity);
                    entityManager.flush();
                    entityManager.clear();
                    return true;
                } catch (RuntimeException | ReflectiveOperationException ex) {
                    status.setRollbackOnly();
                    log.debug("Unable to auto-seed {}: {}", type.getSimpleName(), ex.getMessage());
                    return false;
                }
            });

            return Boolean.TRUE.equals(result);
        } catch (RuntimeException ex) {
            log.debug("Unable to commit demo seed for {}: {}", type.getSimpleName(), ex.getMessage());
            return false;
        }
    }

    private long count(Class<?> type) {
        String entityName = entityManager.getMetamodel().entity(type).getName();
        return entityManager
            .createQuery("select count(e) from " + entityName + " e", Long.class)
            .getSingleResult();
    }

    private Object instantiate(Class<?> type, Set<Class<?>> stack)
            throws ReflectiveOperationException {

        if (Modifier.isAbstract(type.getModifiers()) || stack.contains(type)) return null;

        stack.add(type);
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object entity = constructor.newInstance();

            for (Field field : fields(type)) {
                if (skip(field)) continue;
                field.setAccessible(true);

                if (isAssociation(field)) {
                    Object related = related(field.getType(), stack, field.isAnnotationPresent(OneToOne.class));
                    if (related != null) field.set(entity, related);
                    continue;
                }

                if (Collection.class.isAssignableFrom(field.getType())
                        || Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                Object value = syntheticValue(field, type);
                if (value != null) field.set(entity, value);
            }

            return entity;
        } finally {
            stack.remove(type);
        }
    }

    private Object related(Class<?> targetType, Set<Class<?>> stack, boolean preferFresh)
            throws ReflectiveOperationException {

        if (!preferFresh) {
            Object current = first(targetType);
            if (current != null) return current;
        }

        if (stack.contains(targetType) || Modifier.isAbstract(targetType.getModifiers())) {
            return first(targetType);
        }

        Object created = instantiate(targetType, stack);
        if (created == null) return first(targetType);

        entityManager.persist(created);
        entityManager.flush();
        return created;
    }

    private Object first(Class<?> type) {
        try {
            String entityName = entityManager.getMetamodel().entity(type).getName();
            List<?> rows = entityManager.createQuery("select e from " + entityName + " e")
                .setMaxResults(1)
                .getResultList();
            return rows.isEmpty() ? null : rows.get(0);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<Field> fields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            result.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return result;
    }

    private boolean skip(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) return true;
        if (field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(Version.class)
                || field.isAnnotationPresent(Transient.class)) return true;

        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.insertable()) return true;

        return Set.of("createdAt", "updatedAt").contains(field.getName());
    }

    private boolean isAssociation(Field field) {
        return field.isAnnotationPresent(ManyToOne.class)
            || field.isAnnotationPresent(OneToOne.class);
    }

    private String didacticStatus(String owner) {
        return switch (owner) {
            case "Custody" -> "ACTIVE";
            case "AmmunitionConsumption", "Donation", "DisposalProcess", "InventorySale" -> "FINALIZED";
            case "InventoryTransfer" -> "SENT";
            case "WorkOrder" -> "OPEN";
            case "ApprovalWorkflow" -> "AUTHORIZED";
            case "ExceptionOccurrence" -> "OPEN";
            default -> "ACTIVE";
        };
    }

    private String didacticType(String owner, String fieldName) {
        if (owner.equals("AmmunitionConsumption")) return "TRAINING";
        if (owner.equals("InventoryTransfer")) return "INTERNAL";
        if (owner.equals("Custody")) return "INDIVIDUAL";
        if (owner.equals("ExceptionOccurrence")) return "DIVERGENCE";
        if (owner.equals("MaintenancePlan")) return "PREVENTIVE";
        if (owner.equals("WorkOrder")) return "PREVENTIVE";
        if (fieldName.contains("actor")) return "USER";
        return "DEMO";
    }

    private Object syntheticValue(Field field, Class<?> owner) {
        Class<?> type = field.getType();
        long n = sequence.incrementAndGet();
        String name = field.getName().toLowerCase(Locale.ROOT);

        if (type == String.class) {
            String ownerName = owner.getSimpleName();
            String value;

            if (name.equals("requestid") || name.equals("completionrequestid")) {
                value = UUID.nameUUIDFromBytes((ownerName + "-" + n).getBytes()).toString();
            } else if (name.contains("fingerprint")) {
                value = String.format("%064x", n);
            } else if (name.contains("email")) {
                value = "teste." + ownerName.toLowerCase(Locale.ROOT) + n + "@comandos.local";
            } else if (name.equals("taxid") || name.contains("tax_id")) {
                value = String.format("%011d", 90000000000L + (n % 9999999999L));
            } else if (name.contains("json")) {
                value = "{\"exemplo\":\"Dado didático do COMANDOS\"}";
            } else if (name.contains("url")) {
                value = "https://example.local/comandos/demo/" + ownerName.toLowerCase(Locale.ROOT) + "/" + n;
            } else if (name.contains("countrycode")) {
                value = "BR";
            } else if (name.equals("state") || name.endsWith("statecode")) {
                value = "GO";
            } else if (name.equals("country")) {
                value = "Brasil";
            } else if (name.contains("postalcode") || name.contains("zipcode")) {
                value = "74000-000";
            } else if (name.contains("city")) {
                value = "Goiânia";
            } else if (name.contains("district")) {
                value = "Setor Central";
            } else if (name.contains("street") || name.contains("address")) {
                value = "Avenida Goiás, 1000";
            } else if (name.contains("phone") || name.contains("number") && ownerName.contains("Phone")) {
                value = "(62) 99999-0000";
            } else if (name.equals("fullname") || name.equals("recipientname") || name.equals("responsiblename")) {
                value = "Carlos Henrique Souza - teste";
            } else if (name.equals("authorizername")) {
                value = "Mariana Alves Ferreira - teste";
            } else if (name.equals("buyername")) {
                value = "Comprador Institucional - teste";
            } else if (name.equals("donorname")) {
                value = "Órgão Doador - teste";
            } else if (name.equals("doneename")) {
                value = "Órgão Donatário - teste";
            } else if (name.equals("organizationname")) {
                value = "Secretaria de Segurança Pública - Ambiente de Teste";
            } else if (name.equals("unitname") || name.endsWith("unitname")) {
                value = "Armamento Central";
            } else if (name.equals("locationname") || name.endsWith("locationname")) {
                value = "Cofre Central";
            } else if (name.equals("modelname")) {
                value = "Beretta APX A1 Full Size";
            } else if (name.equals("assetcode") || name.equals("stockcode")) {
                value = "PAT-DEMO-" + n;
            } else if (name.equals("serialnumber")) {
                value = "SER-DEMO-" + n;
            } else if (name.equals("sku")) {
                value = "SKU-DEMO-" + n;
            } else if (name.equals("lotnumber")) {
                value = "LOTE-CBC-2026-" + n;
            } else if (name.contains("processnumber")) {
                value = "PROC-DEMO-2026-" + n;
            } else if (name.contains("documentnumber")) {
                value = "DOC-DEMO-" + n;
            } else if (name.contains("invoicenumber")) {
                value = "NF-DEMO-" + n;
            } else if (name.contains("documentreference") || name.contains("storagereference")) {
                value = "SEI-DEMO-" + n;
            } else if (name.equals("purpose")) {
                value = "Treinamento operacional e validação didática do fluxo";
            } else if (name.contains("justification")) {
                value = "Necessidade fictícia criada para demonstrar o fluxo completo do COMANDOS.";
            } else if (name.contains("reason")) {
                value = "Exemplo didático para treinamento e homologação.";
            } else if (name.contains("notes") || name.contains("description") || name.contains("observation")) {
                value = "Registro fictício e didático para demonstrar esta etapa do sistema.";
            } else if (name.contains("checklist")) {
                value = "Numeração; integridade; funcionamento; acessórios; condição geral";
            } else if (name.contains("result")) {
                value = "CONFORME";
            } else if (name.contains("method")) {
                value = "Fragmentação controlada - demonstração";
            } else if (name.contains("certificate")) {
                value = "CERT-DEMO-" + n;
            } else if (name.contains("status")) {
                value = didacticStatus(ownerName);
            } else if (name.contains("action")) {
                value = "CREATE";
            } else if (name.contains("scope")) {
                value = "SYSTEM";
            } else if (name.contains("type")) {
                value = didacticType(ownerName, name);
            } else if (name.contains("login") || name.contains("operator") || name.contains("inspector")
                    || name.contains("gunsmith") || name.contains("receivedby") || name.contains("incorporatedby")) {
                value = "usuario.teste";
            } else if (name.contains("password")) {
                value = "{noop}Teste@123";
            } else if (name.equals("code")) {
                value = ownerName.toUpperCase(Locale.ROOT) + "_DEMO_" + n;
            } else if (name.equals("name") || name.equals("title")) {
                value = "Exemplo didático - " + ownerName;
            } else {
                value = "Exemplo didático " + ownerName + " - " + field.getName() + " " + n;
            }

            Column column = field.getAnnotation(Column.class);
            int max = column == null ? 255 : column.length();
            return value.substring(0, Math.min(value.length(), Math.max(max, 1)));
        }

        if (type == Boolean.class || type == boolean.class) return true;
        if (type == Integer.class || type == int.class) return 1;
        if (type == Long.class || type == long.class) return n;
        if (type == Short.class || type == short.class) return (short) 1;
        if (type == Byte.class || type == byte.class) return (byte) 1;
        if (type == Double.class || type == double.class) return 1d;
        if (type == Float.class || type == float.class) return 1f;
        if (type == BigDecimal.class) return BigDecimal.ONE;
        if (type == LocalDate.class) return LocalDate.now().plusYears(1);
        if (type == LocalDateTime.class) return LocalDateTime.now();
        if (type == Instant.class) return Instant.now();
        if (type == OffsetDateTime.class) return OffsetDateTime.now();
        if (type == ZonedDateTime.class) return ZonedDateTime.now();
        if (type == UUID.class) return UUID.randomUUID();
        if (type == byte[].class) return new byte[] { 1 };
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length == 0 ? null : constants[0];
        }

        return null;
    }
}
