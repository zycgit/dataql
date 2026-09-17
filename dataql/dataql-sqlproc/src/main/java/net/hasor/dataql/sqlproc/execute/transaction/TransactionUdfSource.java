/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.transaction;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;

/** Transaction functions imported with {@code import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran}. */
public class TransactionUdfSource extends AbstractUdfSource {
    private final HostContext context;

    public TransactionUdfSource() {
        this.context = null;
    }

    public TransactionUdfSource(HostContext context) {
        this.context = context;
    }

    @Override
    public UdfSource create(HostContext context) {
        return new TransactionUdfSource(context);
    }

    @Override
    public <T> T get(Class<? extends T> targetType) {
        return targetType.cast(this);
    }

    public Object required(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.REQUIRED);
    }

    public Object requiresNew(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.REQUIRES_NEW);
    }

    public Object nested(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.NESTED);
    }

    public Object supports(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.SUPPORTS);
    }

    public Object notSupported(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.NOT_SUPPORTED);
    }

    public Object never(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.NEVER);
    }

    public Object mandatory(Udf udf, Hints hints) throws Throwable {
        return this.execute(udf, hints, Propagation.MANDATORY);
    }

    /** Compatibility alias retained from the original FunctionX UDF. */
    public Object tranMandatory(Udf udf, Hints hints) throws Throwable {
        return this.mandatory(udf, hints);
    }

    private Object execute(Udf udf, Hints hints, Propagation propagation) throws Throwable {
        String sourceName = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_DATA_SOURCE);
        Isolation isolation = this.resolveIsolation(hints);

        TransactionManager txManager = this.findTransactionConnectionProvider().findTransactionManager(sourceName);
        TransactionStatus status = txManager.begin(hints, propagation, isolation);
        try {
            return udf.call(hints);
        } catch (Throwable e) {
            status.setRollback();
            throw e;
        } finally {
            if (!status.isCompleted()) {
                txManager.commit(status);
            }
        }
    }

    private Isolation resolveIsolation(Hints hints) {
        String isolation = SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_TRANSACTION_ISOLATION);
        if (isolation == null || isolation.isBlank()) {
            return Isolation.DEFAULT;
        }
        return Isolation.valueOf(isolation.trim().toUpperCase().replace('-', '_'));
    }

    private TransactionProvider findTransactionConnectionProvider() {
        if (this.context == null) {
            throw new IllegalStateException("TransactionUdfSource must be created by HostContext.");
        }
        ConnectionProvider provider = this.context.getAttachment(ConnectionProvider.class);
        if (provider instanceof TransactionProvider tx) {
            return tx;
        }
        throw new IllegalStateException("ConnectionProvider must be TransactionConnectionProvider when using transaction functions.");
    }
}
