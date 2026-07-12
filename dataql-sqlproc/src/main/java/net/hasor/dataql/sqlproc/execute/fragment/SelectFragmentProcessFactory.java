package net.hasor.dataql.sqlproc.execute.fragment;

public final class SelectFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public SelectFragmentProcessFactory() {
        super("select", SelectFragmentProcess::new);
    }
}
