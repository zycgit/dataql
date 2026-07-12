package net.hasor.dataql.sqlproc.execute.support;

public final class DeleteFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public DeleteFragmentProcessFactory() {
        super("delete", DeleteFragmentProcess::new);
    }
}
