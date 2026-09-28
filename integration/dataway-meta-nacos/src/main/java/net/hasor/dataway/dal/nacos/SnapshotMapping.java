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
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Translates storage names at the JSON boundary; snapshot operations keep their logical fields. */
final class SnapshotMapping {
    private final Map<EntityType, String>                tables       = new EnumMap<>(EntityType.class);
    private final Map<EntityType, Map<String, FieldDef>> fields       = new EnumMap<>(EntityType.class);
    private final Map<EntityType, Set<String>>           unusedFields = new EnumMap<>(EntityType.class);

    public SnapshotMapping(Map<EntityType, String> tables, Map<EntityType, Map<FieldDef, String>> fields) {
        for (EntityType type : EntityType.values()) {
            this.tables.put(type, this.requireName(tables.getOrDefault(type, type.name())));
            Map<FieldDef, String> overrides = fields.getOrDefault(type, Map.of());
            NacosSnapshot.validateFields(type, overrides.keySet());
            Map<String, FieldDef> names = new LinkedHashMap<>();
            Set<String> unused = new HashSet<>();
            for (FieldDef field : FieldDef.values()) {
                boolean supported = switch (field) {
                    case API_ID, RELEASE_TIME -> type == EntityType.RELEASE;
                    case CREATE_TIME, GMT_TIME -> type == EntityType.INFO;
                    default -> true;
                };
                if (!supported) {
                    continue;
                }
                String name = this.requireName(overrides.getOrDefault(field, field.name()));
                if (names.put(name, field) != null) {
                    throw new IllegalArgumentException("Duplicate field name in " + type + ": " + name);
                }
                unused.add(field.name());
            }
            unused.removeAll(names.keySet());
            this.fields.put(type, names);
            this.unusedFields.put(type, unused);
        }
        if (new HashSet<>(this.tables.values()).size() != this.tables.size()) {
            throw new IllegalArgumentException("Duplicate Nacos entity name");
        }
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nacos storage names must not be blank");
        }
        return name;
    }

    public ObjectNode parse(String content) {
        if (content == null || content.isBlank()) {
            throw new DataAccessException("Nacos snapshot is missing; provision an empty snapshot before use", null);
        }
        try {
            return this.object(JsonUtils.readTree(content));
        } catch (RuntimeException e) {
            throw new DataAccessException("Invalid Nacos snapshot; refusing to overwrite it", e);
        }
    }

    private ObjectNode object(JsonNode node) {
        if (!(node instanceof ObjectNode object)) {
            throw new IllegalArgumentException("Expected an object in Nacos snapshot");
        }
        return object;
    }

    public NacosSnapshot read(ObjectNode document) {
        try {
            ObjectNode stored = this.object(document.get("records"));
            if (stored.size() != this.tables.size()) {
                throw new IllegalArgumentException("Invalid snapshot entities");
            }
            ObjectNode records = stored.objectNode();
            for (EntityType type : EntityType.values()) {
                ObjectNode rows = this.object(stored.get(this.tables.get(type)));
                ObjectNode normalized = rows.objectNode();
                for (var entry : rows.properties()) {
                    ObjectNode row = this.object(entry.getValue());
                    ObjectNode values = row.objectNode();
                    for (var field : row.properties()) {
                        FieldDef logical = this.fields.get(type).get(field.getKey());
                        if (logical != null) {
                            values.set(logical.name(), field.getValue());
                        } else if (!this.unusedFields.get(type).contains(field.getKey())) {
                            throw new IllegalArgumentException("Unknown field in " + type + ": " + field.getKey());
                        }
                    }
                    normalized.set(entry.getKey(), values);
                }
                records.set(type.name(), normalized);
            }
            ObjectNode normalized = document.objectNode().setAll(document);
            normalized.set("records", records);
            NacosSnapshot snapshot = JsonUtils.convertValue(normalized, NacosSnapshot.class);
            snapshot.validate();
            return snapshot;
        } catch (RuntimeException e) {
            throw new DataAccessException("Invalid Nacos snapshot; refusing to overwrite it", e);
        }
    }

    public void removeRecord(ObjectNode document, EntityType type, String id) {
        ObjectNode records = this.object(document.get("records"));
        this.object(records.get(this.tables.get(type))).remove(id);
    }

    public String serialize(NacosSnapshot snapshot, ObjectNode original) {
        snapshot.validate();
        ObjectNode document = JsonUtils.convertValue(snapshot, ObjectNode.class);
        ObjectNode logical = this.object(document.get("records"));
        ObjectNode previous = this.object(original.get("records"));
        ObjectNode records = logical.objectNode();
        for (EntityType type : EntityType.values()) {
            String table = this.tables.get(type);
            ObjectNode rows = this.object(logical.get(type.name()));
            ObjectNode oldRows = this.object(previous.get(table));
            ObjectNode stored = rows.objectNode();
            for (var entry : rows.properties()) {
                ObjectNode row = this.object(entry.getValue());
                ObjectNode values = row.objectNode();
                JsonNode oldRow = oldRows.get(entry.getKey());
                if (oldRow != null) {
                    for (String unused : this.unusedFields.get(type)) {
                        if (oldRow.has(unused)) {
                            values.set(unused, oldRow.get(unused));
                        }
                    }
                }
                for (var field : this.fields.get(type).entrySet()) {
                    JsonNode value = row.get(field.getValue().name());
                    if (value != null) {
                        values.set(field.getKey(), value);
                    }
                }
                stored.set(entry.getKey(), values);
            }
            records.set(table, stored);
        }
        document.set("records", records);
        return JsonUtils.writeValueAsString(document);
    }
}
