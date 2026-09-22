/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;

/** Storage-neutral fields. SCHEMA, SAMPLE and OPTION retain their complete legacy JSON documents. */
public enum FieldDef {
    ID,
    API_ID,
    METHOD,
    PATH,
    STATUS,
    COMMENT,
    TYPE,
    SCRIPT,
    SCRIPT_ORI,
    SCHEMA,
    SAMPLE,
    OPTION,
    CREATE_TIME,
    GMT_TIME,
    RELEASE_TIME,
    REVISION
}
