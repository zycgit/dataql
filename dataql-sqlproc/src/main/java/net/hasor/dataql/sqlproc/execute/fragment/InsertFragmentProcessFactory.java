package net.hasor.dataql.sqlproc.execute.fragment;

public final class InsertFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public InsertFragmentProcessFactory() {
        super("insert", InsertFragmentProcess::new);
    }
}
