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
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Complements the curated demo data with one synthetic row for every empty
 * application entity table that can be safely instantiated from JPA metadata.
 *
 * Runs only in the demo profile. Each entity is seeded in its own transaction,
 * so one unsupported relationship never rolls back the other demo records.
 */
@Component
@Profile("demo")
@Order(100)
public class DemoCoverageSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCoverageSeeder.class);

    private final EntityManager entityManager;
    private final TransactionTemplate transactions;
    private final AtomicLong sequence = new AtomicLong(1000);

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
            log.warn(
                "Demo coverage seeder could not auto-populate {} mapped tables: {}",
                pending.size(),
                pending.stream().map(Class::getSimpleName).sorted().toList()
            );
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

    private Object syntheticValue(Field field, Class<?> owner) {
        Class<?> type = field.getType();
        long n = sequence.incrementAndGet();
        String name = field.getName().toLowerCase(Locale.ROOT);

        if (type == String.class) {
            String value;
            if (name.contains("email")) {
                value = "demo" + n + "@example.local";
            } else if (name.equals("taxid") || name.contains("tax_id")) {
                value = String.format("%011d", n % 100_000_000_000L);
            } else if (name.contains("json")) {
                value = "{}";
            } else if (name.contains("url")) {
                value = "https://example.local/demo/" + n;
            } else if (name.contains("countrycode")) {
                value = "BR";
            } else if (name.contains("state")) {
                value = "GO";
            } else if (name.contains("status")) {
                value = "ACTIVE";
            } else if (name.contains("action")) {
                value = "CREATE";
            } else if (name.contains("scope")) {
                value = "SYSTEM";
            } else if (name.contains("type")) {
                value = "DEMO";
            } else if (name.contains("login")) {
                value = "demo-" + n;
            } else if (name.contains("password")) {
                value = "{noop}demo";
            } else {
                value = "Demo " + owner.getSimpleName() + " " + field.getName() + " " + n;
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
