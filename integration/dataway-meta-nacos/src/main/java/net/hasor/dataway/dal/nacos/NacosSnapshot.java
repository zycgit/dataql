/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.*;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.dal.*;

/** One CAS unit, including all draft routes and release history. */
public class NacosSnapshot {
    private Integer                                             format;
    private String                                              generation;
    private Map<EntityType, Map<String, Map<FieldDef, String>>> records;

    public NacosSnapshot() {
    }

    /** Creates the initial storage document without relying on a bundled JSON resource. */
    public static NacosSnapshot empty() {
        NacosSnapshot snapshot = new NacosSnapshot();
        snapshot.setFormat(1);
        snapshot.setGeneration("initial");
        Map<EntityType, Map<String, Map<FieldDef, String>>> records = new EnumMap<>(EntityType.class);
        for (EntityType type : EntityType.values()) {
            records.put(type, new LinkedHashMap<>());
        }
        snapshot.setRecords(records);
        return snapshot;
    }

    public Integer getFormat() {
        return format;
    }

    public void setFormat(Integer format) {
        this.format = format;
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public Map<EntityType, Map<String, Map<FieldDef, String>>> getRecords() {
        return records;
    }

    public void setRecords(Map<EntityType, Map<String, Map<FieldDef, String>>> records) {
        this.records = records;
    }

    public static NacosSnapshot parse(String content) {
        if (content == null || content.isBlank()) {
            throw new DataAccessException("Nacos snapshot is missing; provision a serialized NacosSnapshot.empty() before use", null);
        }
        try {
            NacosSnapshot snapshot = JsonUtils.readValue(content, NacosSnapshot.class);
            Objects.requireNonNull(snapshot, "snapshot").validate();
            return snapshot;
        } catch (RuntimeException e) {
            throw new DataAccessException("Invalid Nacos snapshot; refusing to overwrite it", e);
        }
    }

    private void validate() {
        if (!Integer.valueOf(1).equals(format) || generation == null || generation.isBlank()) {
            throw new IllegalArgumentException("Unsupported Nacos snapshot format");
        }
        if (records == null || !records.keySet().equals(EnumSet.allOf(EntityType.class))) {
            throw new IllegalArgumentException("Invalid snapshot entities");
        }
        for (EntityType type : EntityType.values()) {
            for (var entry : Objects.requireNonNull(records.get(type), "entity records").entrySet()) {
                String id = entry.getKey();
                Map<FieldDef, String> row = Objects.requireNonNull(entry.getValue(), "record");
                validateFields(type, row.keySet());
                if (id == null || !Objects.equals(id, row.get(FieldDef.ID)) || id.isBlank() || id.length() > 64 || Long.parseLong(row.get(FieldDef.REVISION)) < 1) {
                    throw new IllegalArgumentException("Invalid record identity or revision");
                }
            }
        }
        validateRoutes();
    }

    public static void validateFields(EntityType type, Set<FieldDef> fields) {
        Objects.requireNonNull(type, "entityType");
        for (FieldDef field : fields) {
            Objects.requireNonNull(field, "field");
            boolean supported = switch (field) {
                case API_ID, RELEASE_TIME -> type == EntityType.RELEASE;
                case CREATE_TIME, GMT_TIME -> type == EntityType.INFO;
                default -> true;
            };

            if (!supported) {
                throw new IllegalArgumentException("Unsupported field " + field + " for " + type);
            }
        }
    }

    public void apply(DataMutation mutation) {
        mutation.validate();
        validateFields(mutation.getEntityType(), mutation.getFields().keySet());
        Map<String, Map<FieldDef, String>> rows = records.get(mutation.getEntityType());
        Map<FieldDef, String> old = rows.get(mutation.getId());
        if (mutation.getOperationType() == OperationType.CREATE) {
            if (old != null) {
                throw new DataConflictException("Dataway record already exists");
            }
        } else if (old == null || Long.parseLong(old.get(FieldDef.REVISION)) != mutation.getVersion()) {
            throw new DataConflictException("Dataway record changed; reload and retry");
        }

        if (mutation.getOperationType() == OperationType.DELETE) {
            rows.remove(mutation.getId());
        } else {
            Map<FieldDef, String> row = new EnumMap<>(FieldDef.class);
            if (old != null) {
                row.putAll(old);
            }
            mutation.getFields().forEach((field, value) -> {
                if (value == null) {
                    row.remove(field);
                } else {
                    row.put(field, value);
                }
            });
            row.put(FieldDef.ID, mutation.getId());
            row.put(FieldDef.REVISION, Long.toString(Math.addExact(mutation.getVersion(), 1)));
            rows.put(mutation.getId(), row);
        }

        validateRoutes();
    }

    private void validateRoutes() {
        Set<List<String>> routes = new HashSet<>();
        for (Map<FieldDef, String> row : records.get(EntityType.INFO).values()) {
            String method = row.get(FieldDef.METHOD);
            String path = row.get(FieldDef.PATH);
            if (method == null || path == null) {
                throw new IllegalArgumentException("INFO requires METHOD and PATH");
            }
            if (!routes.add(List.of(method, path))) {
                throw new DataConflictException("Dataway route (method, path) already exists");
            }
        }
    }

    public String serialize() {
        validate();
        return JsonUtils.writeValueAsString(this);
    }
}
