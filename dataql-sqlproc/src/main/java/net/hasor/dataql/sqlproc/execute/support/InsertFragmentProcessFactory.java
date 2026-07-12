package net.hasor.dataql.sqlproc.execute.support;

public final class InsertFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public InsertFragmentProcessFactory() {
        super("insert", InsertFragmentProcess::new);
    }
}
