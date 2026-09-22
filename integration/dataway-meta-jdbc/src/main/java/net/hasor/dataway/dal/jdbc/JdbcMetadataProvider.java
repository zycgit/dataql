/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import javax.sql.DataSource;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.MetadataContext;
import net.hasor.dataway.dal.MetadataProvider;

/** JDBC SPI; framework transaction policies are supplied through the host context. */
public class JdbcMetadataProvider implements MetadataProvider {
    @Override
    public String getName() {
        return "jdbc";
    }

    @Override
    public ApiDataAccessLayer create(MetadataContext context) {
        String name = context.getProperty("dataway.metadata.jdbc.executor", "");
        JdbcExecutor executor = context.getBean(name, JdbcExecutor.class);
        if (executor == null) {
            String sourceName = context.getProperty("dataway.metadata.jdbc.data-source", "");
            DataSource source = context.getBean(sourceName, DataSource.class);
            if (source == null) {
                throw new IllegalStateException("JDBC metadata requires a DataSource or JdbcExecutor");
            }
            executor = new LocalJdbcExecutor(source);
        }

        return new JdbcDataAccessLayer(executor, context.getProperty("dataway.metadata.jdbc.table-prefix", ""));
    }
}
