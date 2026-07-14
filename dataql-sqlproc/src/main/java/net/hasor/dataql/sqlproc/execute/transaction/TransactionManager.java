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
import java.io.Closeable;
import java.sql.SQLException;
import net.hasor.dataql.domain.Hints;

/** Programmatic transaction manager. */
public interface TransactionManager extends Closeable {
    TransactionStatus begin(Hints hints, Propagation propagation, Isolation isolation) throws SQLException;

    void commit(TransactionStatus status) throws SQLException;

    void rollBack(TransactionStatus status) throws SQLException;

    boolean isTopTransaction(TransactionStatus status);
}
