/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import java.sql.Connection;
import static net.hasor.dataql.sqlproc.SqlHintValue.*;

/** JDBC transaction isolation level. */
public enum Isolation {
    DEFAULT(FRAGMENT_SQL_TRANSACTION_ISOLATION_DEFAULT, Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(FRAGMENT_SQL_TRANSACTION_ISOLATION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(FRAGMENT_SQL_TRANSACTION_ISOLATION_READ_COMMITTED, Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(FRAGMENT_SQL_TRANSACTION_ISOLATION_REPEATABLE_READ, Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(FRAGMENT_SQL_TRANSACTION_ISOLATION_SERIALIZABLE, Connection.TRANSACTION_SERIALIZABLE);

    private final String code;
    private final int    value;

    Isolation(String code, int value) {
        this.code = code;
        this.value = value;
    }

    public String getCode() {
        return this.code;
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
