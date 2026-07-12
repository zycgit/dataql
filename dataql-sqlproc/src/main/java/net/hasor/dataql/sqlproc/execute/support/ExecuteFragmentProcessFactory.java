package net.hasor.dataql.sqlproc.execute.support;

public final class ExecuteFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public ExecuteFragmentProcessFactory() {
        super("execute", ExecuteFragmentProcess::new);
    }
}
