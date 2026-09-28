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

/** Reads the old directory format without exposing it as a writable backend. */
final class LegacyNacosReader {
    private final NacosDataAccessLayer target;
    private final String               group;

    public LegacyNacosReader(NacosDataAccessLayer target, String group) {
        this.target = target;
        this.group = group;
    }

    public List<DataMutation> read() {
        String monitor = target.load("INDEX_MONITOR", group);
        Set<String> ids = new HashSet<>();
        List<DataMutation> batch = new ArrayList<>();
        boolean finished = false;
        for (int page = 0; !finished; page++) {
            if (page >= 10000) {
                throw new DataAccessException("Legacy directory has no END marker", null);
            }

            String directory = target.load("INDEX_DIRECTORY_" + page, group);
            if (directory == null || directory.isBlank()) {
                throw new DataAccessException("Incomplete legacy directory at page " + page, null);
            }

            for (String line : directory.split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                if (line.trim().equalsIgnoreCase("END")) {
                    finished = true;
                    break;
                }
                String[] parts = line.split(",", 3);
                if (parts.length != 3 || !ids.add(parts[0].trim())) {
                    throw new DataAccessException("Invalid or duplicate legacy directory entry", null);
                }

                String id = parts[0].trim();
                EntityType type;
                if (id.startsWith("i_")) {
                    type = EntityType.INFO;
                } else if (id.startsWith("r_")) {
                    type = EntityType.RELEASE;
                } else {
                    throw new DataAccessException("Unknown legacy record type: " + id, null);
                }
                batch.add(target.create(type, OperationType.CREATE, id, 0, fields(type, id)));
            }
        }

        if (!Objects.equals(monitor, target.load("INDEX_MONITOR", group))) {
            throw new DataConflictException("Legacy data changed during import; stop legacy writers first");
        }

        return batch;
    }

    private Map<FieldDef, String> fields(EntityType type, String id) {
        String content = target.load(id, group);
        if (content == null) {
            throw new DataAccessException("Missing legacy record: " + id, null);
        }

        try {
            Map<?, ?> source = JsonUtils.readValue(content, Map.class);
            Map<String, Object> legacy = new LinkedHashMap<>();
            source.forEach((key, value) -> legacy.put(((String) key).toUpperCase(Locale.ROOT), value));
            if (!id.equals(legacy.get("ID"))) {
                throw new IllegalArgumentException("Legacy ID does not match its directory entry");
            }
            Object originalScript = legacy.remove("SCRIPT_ORI");
            if (originalScript != null) {
                legacy.put("SCRIPT", originalScript);
            }

            Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
            for (FieldDef field : FieldDef.values()) {
                if (field != FieldDef.ID && field != FieldDef.REVISION && legacy.containsKey(field.name())) {
                    Object value = legacy.get(field.name());
                    fields.put(field, value == null ? null : value.toString());
                }
            }

            // Legacy Nacos saved the controller's whole map, including fields omitted by the SQL provider.
            if (type == EntityType.INFO) {
                fields.remove(FieldDef.API_ID);
                fields.remove(FieldDef.RELEASE_TIME);
            } else {
                fields.remove(FieldDef.CREATE_TIME);
                fields.remove(FieldDef.GMT_TIME);
            }
            combine(fields, FieldDef.SCHEMA, legacy, "SCHEMA");
            combine(fields, FieldDef.SAMPLE, legacy, "SAMPLE");
            if (legacy.containsKey("PREPARE_HINT")) {
                Map<String, Object> options = object(fields.get(FieldDef.OPTION));
                options.put("PREPARE_HINT", legacy.get("PREPARE_HINT"));
                fields.put(FieldDef.OPTION, JsonUtils.writeValueAsString(options));
            }
            NacosSnapshot.validateFields(type, fields.keySet());
            return fields;
        } catch (RuntimeException e) {
            throw new DataAccessException("Invalid legacy record: " + id, e);
        }
    }

    private void combine(Map<FieldDef, String> fields, FieldDef field, Map<String, Object> legacy, String suffix) {
        Map<String, Object> document = object(fields.get(field));
        String[] old = { "REQ_HEADER_", "REQ_BODY_", "RES_HEADER_", "RES_BODY_" };
        String[] names = { "requestHeader", "requestBody", "responseHeader", "responseBody" };
        for (int i = 0; i < old.length; i++) {
            Object value = legacy.get(old[i] + suffix);
            if (value != null && !value.toString().isBlank()) {
                document.put(names[i], field == FieldDef.SAMPLE ? value.toString() : JsonUtils.readValue(value.toString(), Object.class));
            }
        }

        if (!document.isEmpty()) {
            fields.put(field, JsonUtils.writeValueAsString(document));
        }
    }

    private Map<String, Object> object(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (content != null && !content.isBlank()) {
            Map<?, ?> parsed = JsonUtils.readValue(content, Map.class);
            if (parsed != null) {
                parsed.forEach((key, value) -> result.put((String) key, value));
            }
        }
        return result;
    }
}
