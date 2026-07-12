package net.hasor.dataql.sqlproc.execute.support;

public final class CallFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public CallFragmentProcessFactory() {
        super("call", CallFragmentProcess::new);
    }
}
