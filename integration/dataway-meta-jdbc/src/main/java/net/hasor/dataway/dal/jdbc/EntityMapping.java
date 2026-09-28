/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.*;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;

/** SQL mapping only; no lifecycle or script transformations. */
class EntityMapping {
    private       String                catalog;
    private       String                schema;
    private       String                table;
    private final Map<FieldDef, String> columns;

    public EntityMapping(EntityType entityType, String prefix) {
        this.table = EntityMapping.tableName(entityType, prefix);
        this.columns = EntityMapping.columnMapping(entityType);
    }

    private static String tableName(EntityType entityType, String prefix) {
        return prefix + switch (entityType) {
            case INFO -> "interface_info";
            case RELEASE -> "interface_release";
            default -> throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        };
    }

    private static Map<FieldDef, String> columnMapping(EntityType entityType) {
        String columnPrefix = switch (entityType) {
            case INFO -> "api_";
            case RELEASE -> "pub_";
            default -> throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        };

        Map<FieldDef, String> mapping = new LinkedHashMap<>();
        for (FieldDef field : new FieldDef[] {//
                FieldDef.ID,        //
                FieldDef.METHOD,    //
                FieldDef.PATH,      //
                FieldDef.STATUS,    //
                FieldDef.COMMENT,   //
                FieldDef.TYPE,      //
                FieldDef.SCRIPT,    //
                FieldDef.SCHEMA,    //
                FieldDef.SAMPLE,    //
                FieldDef.OPTION,    //
                FieldDef.REVISION }) {
            mapping.put(field, columnPrefix + field.name().toLowerCase(Locale.ROOT));
        }

        switch (entityType) {
            case INFO -> {
                mapping.put(FieldDef.CREATE_TIME, "api_create_time");
                mapping.put(FieldDef.GMT_TIME, "api_gmt_time");
            }
            case RELEASE -> {
                mapping.put(FieldDef.API_ID, "pub_api_id");
                mapping.put(FieldDef.RELEASE_TIME, "pub_release_time");
            }
            default -> {
                throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
        }
        return mapping;
    }

    public String getCatalog() {
        return this.catalog;
    }

    public String getSchema() {
        return this.schema;
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String table) {
        if (table == null || !table.matches("[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*){0,2}")) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
        String[] parts = table.split("\\.");
        this.catalog = parts.length == 3 ? parts[0] : null;
        this.schema = parts.length > 1 ? parts[parts.length - 2] : null;
        this.table = parts[parts.length - 1];
    }

    public Map<FieldDef, String> getColumns() {
        return this.columns;
    }

    public void mapFields(Map<FieldDef, String> fields) {
        for (var entry : fields.entrySet()) {
            this.column(entry.getKey());
            String name = entry.getValue();
            if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new IllegalArgumentException("Invalid column name for " + entry.getKey() + ": " + name);
            }
            this.columns.put(entry.getKey(), name);
        }

        Set<String> names = new HashSet<>();
        for (String name : this.columns.values()) {
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate column name in " + this.table + ": " + name);
            }
        }
    }

    public String column(FieldDef field) {
        String column = this.columns.get(field);
        if (column == null) {
            throw new IllegalArgumentException("Unsupported field " + field + " for " + this.table);
        }
        return column;
    }
}
