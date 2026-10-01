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
import javax.sql.DataSource;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.transaction.Isolation;
import net.hasor.dataql.sqlproc.execute.transaction.Propagation;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionCallback;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionalProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/** Uses the application's JDBC transaction managers for SQL and script transaction functions. */
public class SpringTransactionProvider implements TransactionalProvider {
    private final ApplicationContext context;

    public SpringTransactionProvider(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Connection findConnection(String sourceName, Hints hints) throws SQLException {
        DataSource source = this.findDataSource(sourceName);
        return source == null ? null : new TransactionAwareDataSourceProxy(source).getConnection();
    }

    private DataSource findDataSource(String sourceName) {
        DataSource source;
        if (sourceName == null || sourceName.isBlank()) {
            source = this.context.getBean(DataSource.class);
        } else if (this.context.containsBean(sourceName)) {
            source = this.context.getBean(sourceName, DataSource.class);
        } else {
            return null;
        }

        while (source instanceof TransactionAwareDataSourceProxy proxy) {
            source = proxy.getTargetDataSource();
        }
        return source;
    }

    @Override
    public Object execute(String sourceName, Hints hints, Propagation propagation, Isolation isolation, TransactionCallback callback) throws Throwable {
        PlatformTransactionManager manager = this.findTransactionManager(sourceName);
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(this.propagation(propagation));
        definition.setIsolationLevel(isolation == Isolation.DEFAULT ? TransactionDefinition.ISOLATION_DEFAULT : isolation.getValue());

        var status = manager.getTransaction(definition);
        Object result;
        try {
            result = callback.execute();
        } catch (Throwable failure) {
            try {
                manager.rollback(status);
            } catch (Throwable rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
        manager.commit(status);
        return result;
    }

    private PlatformTransactionManager findTransactionManager(String sourceName) {
        DataSource source = this.findDataSource(sourceName);
        if (source == null) {
            throw new IllegalStateException("Data source not configured: " + sourceName);
        }

        PlatformTransactionManager selected = null;
        for (PlatformTransactionManager manager : this.context.getBeansOfType(PlatformTransactionManager.class).values()) {
            if (manager instanceof DataSourceTransactionManager jdbc && jdbc.getDataSource() == source) {
                if (selected != null && selected != manager) {
                    throw new IllegalStateException("Multiple Spring JDBC transaction managers for data source: " + sourceName);
                }
                selected = manager;
            }
        }

        if (selected == null) {
            throw new IllegalStateException("Spring JDBC transaction manager not configured for data source: " + sourceName);
        }
        return selected;
    }

    private int propagation(Propagation propagation) {
        return switch (propagation) {
            case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case NESTED -> TransactionDefinition.PROPAGATION_NESTED;
            case SUPPORTS -> TransactionDefinition.PROPAGATION_SUPPORTS;
            case NOT_SUPPORTED -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
            case NEVER -> TransactionDefinition.PROPAGATION_NEVER;
            case MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY;
        };
    }
}
