package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.function.Function;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.FragmentProcessFactory;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

abstract class AbstractSqlFragmentProcessFactory implements FragmentProcessFactory {
    private final String[]                                  names;
    private final Function<ExecuteContext, FragmentProcess> creator;

    protected AbstractSqlFragmentProcessFactory(String operation, Function<ExecuteContext, FragmentProcess> creator) {
        this.names = new String[] { operation + "Sql", operation + "Xml" };
        this.creator = creator;
    }

    @Override
    public String[] getNames() {
        return this.names.clone();
    }

    @Override
    public FragmentProcess create(String name, HostContext context) {
        return this.creator.apply(context.getAttachment(ExecuteContext.class));
    }
}
