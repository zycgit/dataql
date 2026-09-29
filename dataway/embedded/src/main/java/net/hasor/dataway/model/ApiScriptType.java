/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;

public enum ApiScriptType {
    DATA_QL("DataQL", "dataql"),
    SQL("SQL", "executeSql");

    private final String displayName;
    private final String typeName;

    ApiScriptType(String displayName, String typeName) {
        this.displayName = displayName;
        this.typeName = typeName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public static ApiScriptType fromName(String name) {
        for (ApiScriptType type : values()) {
            if (type.typeName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported script type: " + name);
    }
}
