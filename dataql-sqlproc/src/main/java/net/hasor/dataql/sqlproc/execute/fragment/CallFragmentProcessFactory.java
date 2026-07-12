package net.hasor.dataql.sqlproc.execute.fragment;

public final class CallFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public CallFragmentProcessFactory() {
        super("call", CallFragmentProcess::new);
    }
}
