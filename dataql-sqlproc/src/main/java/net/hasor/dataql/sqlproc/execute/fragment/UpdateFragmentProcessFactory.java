package net.hasor.dataql.sqlproc.execute.fragment;

public final class UpdateFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public UpdateFragmentProcessFactory() {
        super("update", UpdateFragmentProcess::new);
    }
}
