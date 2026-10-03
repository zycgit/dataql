/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.configuration;
import java.util.Map;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import net.hasor.dataway.solon.DatawayPlugin;
import net.hasor.dataway.solon.SolonJdbcExecutor;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.TestApplication;
import net.hasor.dataway.solon.testcase.TestSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HostTransactionTest {
    @ParameterizedTest
    @CsvSource({ "commit,200,1", "rollback,409,0", "sql-failure,503,0" })
    void httpBusinessTransactionsCommitOrRollBackMetadataTogether(String mode, int status, int rows) throws Throwable {
        try (H2Database database = new H2Database()) {
            JdbcExecutor executor = new SolonJdbcExecutor(database.source);
            var access = new JdbcDataAccessLayer(executor);
            try (TestApplication app = new TestApplication(context -> {
                new DatawayPlugin(TestSettings.configuration()).start(context);
                context.app().router().post("/host-transaction", new HostTransactionController(context, executor));
            }, access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
                assertEquals(200, client.login("admin").status);
                var result = client.json("/host-transaction?mode=" + mode, Map.of());
                assertEquals(status, result.status, result.text());
                assertEquals(rows, database.count("interface_info"));
                assertEquals(rows, database.count("interface_release"));
                var published = client.get("/api/transaction");
                assertEquals(rows == 1 ? 200 : 404, published.status, published.text());
                if (rows == 1) {
                    assertEquals("\"committed\"", published.text());
                }
                assertEquals(200, client.get("/admin/api/api-list").status);
            }
        }
    }
}
