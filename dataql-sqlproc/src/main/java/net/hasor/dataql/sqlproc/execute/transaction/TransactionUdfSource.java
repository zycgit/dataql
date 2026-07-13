/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

        if (this.context == null) {
            throw new IllegalStateException("TransactionUdfSource must be created by HostContext.");
        }
        TransactionConnectionProvider txProvider = this.findTransactionConnectionProvider();
        TransactionManager txManager = txProvider.findTransactionManager(sourceName);
        TransactionStatus status = null;
        try {
            status = txManager.begin(hints, propagation, isolation);
            return udf.call(hints);
        } catch (Throwable e) {
            if (status != null) {
                status.setRollback();
            }
            throw e;
        } finally {
            if (status != null && !status.isCompleted()) {
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

    private TransactionConnectionProvider findTransactionConnectionProvider() {
        TransactionConnectionProvider txProvider = this.getAttachment(TransactionConnectionProvider.class);
        if (txProvider != null) {
            return txProvider;
        }

        ConnectionProvider provider = this.getAttachment(ConnectionProvider.class);
        if (provider instanceof TransactionConnectionProvider transactionProvider) {
            return transactionProvider;
        }
        if (provider != null) {
            throw new IllegalStateException("ConnectionProvider must be TransactionConnectionProvider when using transaction functions.");
        } else {
            throw new IllegalStateException("TransactionConnectionProvider must be registered as HostContext attachment.");
        }
    }

    private <T> T getAttachment(Class<T> attachmentType) {
        try {
            return this.context.getAttachment(attachmentType);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
