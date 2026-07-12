package net.hasor.dataql.sqlproc.execute.support;

import java.util.function.Function;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.FragmentProcessFactory;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.internal.SqlProcConfiguration;

abstract class AbstractSqlFragmentProcessFactory implements FragmentProcessFactory {
    private final String[]                                names;
    private final Function<QueryContext, FragmentProcess> creator;

    protected AbstractSqlFragmentProcessFactory(String operation, Function<QueryContext, FragmentProcess> creator) {
        this.names = new String[] { operation + "Sql", operation + "Xml" };
        this.creator = creator;
    }

    @Override
    public String[] getNames() {
        return this.names.clone();
    }

    @Override
    public FragmentProcess create(String name, HostContext context) {
        return this.creator.apply(SqlProcConfiguration.get(context).getQueryContext());
    }
}
