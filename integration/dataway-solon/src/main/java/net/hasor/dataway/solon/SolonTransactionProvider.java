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
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.execute.transaction.Isolation;
import net.hasor.dataql.sqlproc.execute.transaction.Propagation;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionCallback;
import net.hasor.dataql.sqlproc.execute.transaction.TransactionalProvider;
import org.noear.solon.core.AppContext;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranIsolation;
import org.noear.solon.data.tran.TranPolicy;
import org.noear.solon.data.tran.TranUtils;

/** Uses Solon's transaction executor for SQL and script transaction functions. */
public class SolonTransactionProvider implements TransactionalProvider {
    private final AppContext context;

    public SolonTransactionProvider(AppContext context) {
        this.context = context;
    }

    @Override
    public Connection findConnection(String sourceName, Hints hints) throws SQLException {
        DataSource source = sourceName == null || sourceName.isBlank() ? this.context.getBean(DataSource.class) : this.context.getBean(sourceName);
        return source == null ? null : TranUtils.getConnectionProxy(source);
    }

    @Override
    public Object execute(String sourceName, Hints hints, Propagation propagation, Isolation isolation, TransactionCallback callback) throws Throwable {
        TransactionAnno transaction = new TransactionAnno();
        transaction.policy(this.propagation(propagation));
        transaction.isolation(this.isolation(isolation));
        AtomicReference<Object> result = new AtomicReference<>();
        TranUtils.execute(transaction, () -> result.set(callback.execute()));
        return result.get();
    }

    private TranPolicy propagation(Propagation propagation) {
        return switch (propagation) {
            case REQUIRED -> TranPolicy.required;
            case REQUIRES_NEW -> TranPolicy.requires_new;
            case NESTED -> TranPolicy.nested;
            case SUPPORTS -> TranPolicy.supports;
            case NOT_SUPPORTED -> TranPolicy.not_supported;
            case NEVER -> TranPolicy.never;
            case MANDATORY -> TranPolicy.mandatory;
        };
    }

    private TranIsolation isolation(Isolation isolation) {
        return switch (isolation) {
            case DEFAULT -> TranIsolation.unspecified;
            case READ_UNCOMMITTED -> TranIsolation.read_uncommitted;
            case READ_COMMITTED -> TranIsolation.read_committed;
            case REPEATABLE_READ -> TranIsolation.repeatable_read;
            case SERIALIZABLE -> TranIsolation.serializable;
        };
    }
}
