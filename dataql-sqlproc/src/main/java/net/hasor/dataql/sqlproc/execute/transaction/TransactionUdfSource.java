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
import java.util.Objects;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.SqlProcFragmentProcessFactory;

/** Transaction functions imported with {@code import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran}. */
public class TransactionUdfSource extends AbstractUdfSource {
    private final TransactionConnectionManager txManager;

    public TransactionUdfSource() {
        this(SqlProcFragmentProcessFactory.connectionManager());
    }

    public TransactionUdfSource(TransactionConnectionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "connectionManager is null.");
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
        TransactionTemplate template = this.txManager.getTransactionTemplate(sourceName);
        return template.execute(status -> udf.call(hints), propagation);
    }
}
