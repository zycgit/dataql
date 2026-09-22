/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;

import javax.sql.DataSource;
import net.hasor.dataway.dal.MetadataContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/** Loaded only when a JDBC provider requests an executor. */
final class SpringJdbcSupport {
    private SpringJdbcSupport() {
    }

    static Object create(MetadataContext context) {
        DataSource source = context.getBean(context.getProperty("dataway.metadata.jdbc.data-source", ""), DataSource.class);
        if (source == null) {
            throw new IllegalStateException("JDBC metadata requires a DataSource");
        }
        String name = context.getProperty("dataway.metadata.jdbc.transaction-manager", "");
        PlatformTransactionManager manager = context.getBean(name, PlatformTransactionManager.class);
        if (manager == null) {
            manager = new JdbcTransactionManager(source);
        }
        return new SpringJdbcExecutor(source, manager);
    }
}
