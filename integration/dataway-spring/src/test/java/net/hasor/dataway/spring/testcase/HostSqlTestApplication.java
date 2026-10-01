/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.spring.SpringTransactionProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/** Spring owns the data source, JDBC template and transaction-aware SQL connections. */
public final class HostSqlTestApplication implements AutoCloseable {
    private final TestApplication application;

    public HostSqlTestApplication(H2Database database) {
        var config = TestSettings.configuration();
        this.application = new TestApplication(config, database.access, TestSettings.enabled(), beans -> {
            beans.registerBean(DataSource.class, () -> database.source);
            beans.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(beans.getBean(DataSource.class)));
            beans.registerBean(PlatformTransactionManager.class, () -> new DataSourceTransactionManager(beans.getBean(DataSource.class)));
            beans.registerBean(ConnectionProvider.class, () -> new SpringTransactionProvider(beans));
            config.apiInterceptor((call, chain) -> {
                var manager = beans.getBean(PlatformTransactionManager.class);
                var status = manager.getTransaction(new DefaultTransactionDefinition());
                try {
                    Object result = chain.proceed(call);
                    if (Boolean.TRUE.equals(call.parameters().get("rollback"))) {
                        throw new DatawayException(409, "Business transaction rolled back");
                    }
                    manager.commit(status);
                    return result;
                } catch (Exception | Error failure) {
                    if (!status.isCompleted()) {
                        manager.rollback(status);
                    }
                    throw failure;
                }
            });
            config.configureHost(host -> {
                host.addAttachment(ConnectionProvider.class, beans.getBean(ConnectionProvider.class));
            });
        });
        try {
            JdbcTemplate jdbc = this.application.context().getBean(JdbcTemplate.class);
            jdbc.execute("CREATE TABLE example_people (id INT PRIMARY KEY, name VARCHAR(100), balance INT)");
            jdbc.execute("INSERT INTO example_people VALUES (1, 'Alice', 100), (2, 'Bob', 200)");
        } catch (RuntimeException failure) {
            this.application.close();
            throw failure;
        }
    }

    public String baseUrl() {
        return this.application.baseUrl();
    }

    @Override
    public void close() {
        this.application.close();
    }
}
