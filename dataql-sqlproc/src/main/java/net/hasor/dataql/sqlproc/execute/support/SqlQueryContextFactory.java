package net.hasor.dataql.sqlproc.execute.support;

import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.HostAttachmentFactory;

public class SqlQueryContextFactory implements HostAttachmentFactory<ExecuteContext> {
    @Override
    public Class<ExecuteContext> getAttachmentType() {
        return ExecuteContext.class;
    }

    @Override
    public ExecuteContext create(HostContext context) {
        return new ExecuteContextImpl(context.getClassLoader(), null);
    }
}
