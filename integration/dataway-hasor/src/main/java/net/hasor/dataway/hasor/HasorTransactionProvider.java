/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.sql.Connection;
import javax.sql.DataSource;
import net.hasor.core.AppContext;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.transaction.Isolation;
import net.hasor.dataql.sqlproc.execute.transaction.Propagation;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionCallback;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionalProvider;
import net.hasor.dbvisitor.transaction.DataSourceUtils;
import net.hasor.dbvisitor.transaction.TransactionTemplate;

/** Uses the application's named dbVisitor services for SQL and script transaction functions. */
public class HasorTransactionProvider implements TransactionalProvider {
    private final AppContext context;

    public HasorTransactionProvider(AppContext context) {
        this.context = context;
    }

    @Override
    public Connection findConnection(String sourceName, Hints hints) {
        String name = sourceName == null || sourceName.isBlank() ? null : sourceName;
        DataSource source = this.context.findBindingBean(name, DataSource.class);
        return source == null ? null : DataSourceUtils.getConnection(source);
    }

    @Override
    public Object execute(String sourceName, Hints hints, Propagation propagation, Isolation isolation, TransactionCallback callback) throws Throwable {
        String name = sourceName == null || sourceName.isBlank() ? null : sourceName;
        TransactionTemplate transactions = this.context.findBindingBean(name, TransactionTemplate.class);
        if (transactions == null) {
            throw new IllegalStateException("dbVisitor transaction services not configured for data source: " + sourceName);
        }

        var behavior = net.hasor.dbvisitor.transaction.Propagation.valueOf(propagation.name());
        var level = net.hasor.dbvisitor.transaction.Isolation.valueOf(isolation.name());
        return transactions.execute(status -> callback.execute(), behavior, level);
    }
}
