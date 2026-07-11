/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.Connection;

/** JDBC transaction isolation level. */
public enum Isolation {
    DEFAULT(Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int value;

    Isolation(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static Isolation valueOf(int value) {
        return switch (value) {
            case Connection.TRANSACTION_NONE -> DEFAULT;
            case Connection.TRANSACTION_READ_UNCOMMITTED -> READ_UNCOMMITTED;
            case Connection.TRANSACTION_READ_COMMITTED -> READ_COMMITTED;
            case Connection.TRANSACTION_REPEATABLE_READ -> REPEATABLE_READ;
            case Connection.TRANSACTION_SERIALIZABLE -> SERIALIZABLE;
            default -> throw new IllegalStateException("Unsupported transaction isolation level " + value + ".");
        };
    }
}
