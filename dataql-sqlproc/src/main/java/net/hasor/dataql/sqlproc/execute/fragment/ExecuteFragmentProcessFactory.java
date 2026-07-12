package net.hasor.dataql.sqlproc.execute.fragment;

public final class ExecuteFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public ExecuteFragmentProcessFactory() {
        super("execute", ExecuteFragmentProcess::new);
    }
}
