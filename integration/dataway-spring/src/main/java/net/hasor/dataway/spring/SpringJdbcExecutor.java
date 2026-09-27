/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import net.hasor.dataway.dal.jdbc.JdbcCallback;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses Spring connection ownership and REQUIRED propagation for metadata batches. */
public class SpringJdbcExecutor implements JdbcExecutor {
    private final DataSource          source;
    private final TransactionTemplate transactions;

    public SpringJdbcExecutor(DataSource source, PlatformTransactionManager manager) {
        Objects.requireNonNull(source);
        while (source instanceof TransactionAwareDataSourceProxy proxy) {
            source = Objects.requireNonNull(proxy.getTargetDataSource());
        }
        if (manager instanceof DataSourceTransactionManager jdbc && jdbc.getDataSource() != source) {
            throw new IllegalArgumentException("Dataway datasource must match its Spring transaction manager");
        }

        this.source = source;
        this.transactions = new TransactionTemplate(Objects.requireNonNull(manager));
    }

    @Override
    public <T> T execute(JdbcCallback<T> callback) throws SQLException {
        try {
            return transactions.execute(status -> {
                Connection connection = DataSourceUtils.getConnection(source);
                try {
                    return callback.execute(connection);
                } catch (SQLException e) {
                    throw new UncategorizedSQLException("Dataway metadata batch", null, e);
                } finally {
                    DataSourceUtils.releaseConnection(connection, source);
                }
            });
        } catch (UncategorizedSQLException e) {
            throw e.getSQLException();
        }
    }
}
