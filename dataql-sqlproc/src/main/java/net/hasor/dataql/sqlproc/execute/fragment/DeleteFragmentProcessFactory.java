package net.hasor.dataql.sqlproc.execute.fragment;

public final class DeleteFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public DeleteFragmentProcessFactory() {
        super("delete", DeleteFragmentProcess::new);
    }
}
