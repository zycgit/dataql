/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import net.hasor.dataway.dal.jdbc.JdbcCallback;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranUtils;

/** Uses Solon's transaction executor and transaction-aware connection proxy. */
public class SolonJdbcExecutor implements JdbcExecutor {
    private final DataSource      source;
    private final TransactionAnno transaction = new TransactionAnno();

    public SolonJdbcExecutor(DataSource source) {
        this.source = Objects.requireNonNull(source);
    }

    @Override
    public <T> T execute(JdbcCallback<T> callback) throws SQLException {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            TranUtils.execute(transaction, () -> {
                try (Connection connection = TranUtils.getConnectionProxy(source)) {
                    result.set(callback.execute(connection));
                }
            });
            return result.get();
        } catch (SQLException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new SQLException("Solon transaction failed", e);
        }
    }
}
