/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import net.hasor.dataway.dal.jdbc.JdbcCallback;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import net.hasor.dbvisitor.transaction.DataSourceUtils;
import net.hasor.dbvisitor.transaction.TransactionTemplate;
import net.hasor.dbvisitor.transaction.TransactionTemplateManager;
import net.hasor.dbvisitor.transaction.support.TransactionHelper;

/** Uses the Hasor application's dbVisitor connections and REQUIRED transactions for metadata operations. */
public class HasorJdbcExecutor implements JdbcExecutor {
    private final DataSource          source;
    private final TransactionTemplate transactions;

    public HasorJdbcExecutor(DataSource source) {
        this.source = source;
        this.transactions = new TransactionTemplateManager(TransactionHelper.txManager(source));
    }

    @Override
    public <T> T execute(JdbcCallback<T> callback) throws SQLException {
        try {
            return this.transactions.execute(status -> {
                try (Connection connection = DataSourceUtils.getConnection(this.source)) {
                    return callback.execute(connection);
                }
            });
        } catch (SQLException | RuntimeException | Error failure) {
            throw failure;
        } catch (Throwable failure) {
            throw new SQLException("Dataway metadata transaction failed", failure);
        }
    }
}
