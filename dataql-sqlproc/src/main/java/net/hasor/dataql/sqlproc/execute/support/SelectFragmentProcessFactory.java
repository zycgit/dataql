package net.hasor.dataql.sqlproc.execute.support;

public final class SelectFragmentProcessFactory extends AbstractSqlFragmentProcessFactory {
    public SelectFragmentProcessFactory() {
        super("select", SelectFragmentProcess::new);
    }
}
