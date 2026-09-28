/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;

/** SQL mapping only; no lifecycle or script transformations. */
record EntityMapping(String table, Map<FieldDef, String> columns) {
    public EntityMapping(EntityType entityType, String prefix) {
        this(tableName(entityType, prefix), columnMapping(entityType));
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
                mapping.put(FieldDef.SCRIPT_ORI, "pub_script_ori");
                mapping.put(FieldDef.RELEASE_TIME, "pub_release_time");
            }
            default -> {
                throw new IllegalArgumentException("Unsupported entity type: " + entityType);
            }
        }
        return mapping;
    }

    public String column(FieldDef field) {
        String column = columns.get(field);
        if (column == null) {
            throw new IllegalArgumentException("Unsupported field " + field + " for " + table);
        }
        return column;
    }
}
