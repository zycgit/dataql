package net.hasor.dataql.sqlproc.execute.support;

public final class UpdateFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public UpdateFragmentProcessFactory() {
        super("update", UpdateFragmentProcess::new);
    }
}
