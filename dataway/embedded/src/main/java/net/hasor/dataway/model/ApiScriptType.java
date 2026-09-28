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
    SQL("SQL", "sql");

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
}
