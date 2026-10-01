/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.solon.DatawayPlugin;
import net.hasor.dataway.solon.SolonTransactionProvider;
import org.noear.solon.data.annotation.TransactionAnno;
import org.noear.solon.data.tran.TranUtils;

/** Solon owns the data source and supplies connections through its transaction support. */
public final class HostSqlTestApplication implements AutoCloseable {
    private final TestApplication application;

    public HostSqlTestApplication(H2Database database) throws Throwable {
        this.application = new TestApplication(context -> {
            context.wrapAndPut(DataSource.class, database.source);
            ConnectionProvider provider = new SolonTransactionProvider(context);
            context.wrapAndPut(ConnectionProvider.class, provider);
            var config = TestSettings.configuration().configureHost(host -> {
                host.addAttachment(ConnectionProvider.class, context.getBean(ConnectionProvider.class));
            });
            config.apiInterceptor((call, chain) -> {
                AtomicReference<Object> result = new AtomicReference<>();
                try {
                    TranUtils.execute(new TransactionAnno(), () -> {
                        result.set(chain.proceed(call));
                        if (Boolean.TRUE.equals(call.parameters().get("rollback"))) {
                            throw new DatawayException(409, "Business transaction rolled back");
                        }
                    });
                } catch (Exception failure) {
                    throw failure;
                } catch (Throwable failure) {
                    throw new IllegalStateException("Business transaction failed", failure);
                }
                return result.get();
            });
            new DatawayPlugin(config).start(context);
        }, database.access, TestSettings.enabled());
        try (var connection = TranUtils.getConnectionProxy(this.application.context().context().getBean(DataSource.class)); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE example_people (id INT PRIMARY KEY, name VARCHAR(100), balance INT)");
            statement.execute("INSERT INTO example_people VALUES (1, 'Alice', 100), (2, 'Bob', 200)");
        } catch (Exception failure) {
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
