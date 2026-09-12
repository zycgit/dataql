/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
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
        return new ExecuteContextImpl(context, context.getClassLoader());
    }
}
