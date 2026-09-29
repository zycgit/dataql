/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.configuration;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;
import net.hasor.boot.web.WebServer;
import net.hasor.core.AppContext;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.example.ExampleApplication;
import net.hasor.dataway.hasor.example.service.ExampleApiService;
import net.hasor.dataway.hasor.testcase.*;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dbvisitor.jdbc.core.JdbcTemplate;
import net.hasor.dbvisitor.session.Session;
import net.hasor.dbvisitor.transaction.TransactionTemplate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class BootHttpTest {
    @Test
    void standaloneExampleBootsFromXmlAndScannedApplicationConfiguration() throws Exception {
        try (BootApplication application = this.exampleBoot().start()) {
            AppContext context = application.getAppContext();
            this.assertTables(context.getInstance(DataSource.class), Set.of("INTERFACE_INFO", "INTERFACE_RELEASE"));
            this.assertTables(context.findBindingBean("ds1", DataSource.class), Set.of("EXAMPLE_PEOPLE"));
            this.assertTables(context.findBindingBean("ds2", DataSource.class), Set.of("EXAMPLE_ORDERS"));
            int port = application.getAppContext().getInstance(WebServer.class).getPort();
            try (HttpClient client = new HttpClient("http://127.0.0.1:" + port)) {
                assertEquals(200, client.get("/").status);
                assertEquals("/admin/", client.json("/example/config", Map.of()).json().get("admin"));
                assertEquals(401, client.get("/admin/").status);
                assertEquals(200, client.login("admin").status);
                assertEquals(200, client.get("/admin/").status);
                assertEquals(4, this.result(client.get("/admin/api/api-list")).path("result").size());
                JsonNode code = this.result(client.get("/admin/api/api-detail?id=example-person-orders")).path("result").path("codeInfo").path("codeValue");
                assertTrue(code.asText().contains("\"ds1\""), code.toString());
                assertTrue(code.asText().contains("\"ds2\""), code.toString());
                JsonNode saved = this.result(client.json("/admin/api/save-api", Map.of("id", "-1", "version", 0, "select", "POST", "apiPath", "/default-source", "codeType", "SQL", "codeValue", "SELECT api_path FROM interface_info WHERE api_id = #{id}", "requestBody", Map.of("id", "example-person"), "optionInfo", Map.of("resultStructure", false))));
                assertTrue(saved.path("success").asBoolean(), saved.toString());
                JsonNode published = this.result(client.json("/admin/api/publish", Map.of("id", saved.path("result").asText(), "version", 1)));
                assertTrue(published.path("success").asBoolean(), published.toString());
                assertEquals(200, client.login("api").status);
                assertEquals("/person", this.result(client.json("/api/default-source", Map.of("id", "example-person"))).asText());
                JsonNode person = this.structuredResult(client.json("/api/person", Map.of("id", 2)));
                assertEquals("Bob", person.path("name").asText(), person.toString());
                JsonNode people = this.structuredResult(client.json("/api/people", Map.of()));
                assertEquals(2, people.size(), people.toString());
                assertEquals("Alice", people.path(0).path("name").asText());
                JsonNode orders = this.structuredResult(client.json("/api/orders", Map.of("id", 1)));
                assertEquals(2, orders.size(), orders.toString());
                assertEquals("Keyboard", orders.path(0).path("product").asText());

                JsonNode alice = this.structuredResult(client.json("/api/person-orders", Map.of("id", 1)));
                assertEquals("Alice", alice.path("person").path("name").asText(), alice.toString());
                assertEquals(2, alice.path("orders").size());
                JsonNode bob = this.structuredResult(client.json("/api/person-orders", Map.of("id", 2)));
                assertEquals("Bob", bob.path("person").path("name").asText(), bob.toString());
                assertEquals(1, bob.path("orders").size());
                assertEquals("Monitor", bob.path("orders").path(0).path("product").asText());
                assertEquals(0, this.structuredResult(client.json("/api/orders", Map.of("id", 999))).size());

                for (String document : new String[] { "openapi.json", "swagger2.json" }) {
                    JsonNode paths = this.result(client.get("/docs/" + document)).path("paths");
                    for (String path : new String[] { "/person", "/people", "/orders", "/person-orders" }) {
                        assertTrue(paths.path(path).has("post"), paths.toString());
                    }
                }
            }
        }
    }

    private JsonNode structuredResult(HttpResult result) {
        JsonNode response = this.result(result);
        assertTrue(response.path("success").asBoolean(), response.toString());
        assertEquals("OK", response.path("message").asText());
        assertEquals(0, response.path("code").asInt(-1));
        assertTrue(response.path("lifeCycleTime").isNumber());
        assertTrue(response.path("executionTime").isNumber());
        assertTrue(response.has("value"));
        return response.path("value");
    }

    private Boot exampleBoot() {
        String database = "jdbc:h2:mem:" + UUID.randomUUID();
        return new Boot().hconfigFile("example/hconfig.xml").sources(ExampleApplication.class).property("hasor.boot.web.connectors.http.port", 0).property("example.database.main.url", database + "-main").property("example.database.ds1.url", database + "-ds1").property("example.database.ds2.url", database + "-ds2");
    }

    private void assertTables(DataSource source, Set<String> expected) throws Exception {
        Set<String> tables = new HashSet<>();
        try (var connection = source.getConnection(); var rows = connection.getMetaData().getTables(null, "PUBLIC", "%", null)) {
            while (rows.next()) {
                tables.add(rows.getString("TABLE_NAME"));
            }
        }
        assertEquals(expected, tables);
    }

    private JsonNode result(HttpResult result) {
        assertEquals(200, result.status, result.text());
        return JsonUtils.readTree(result.text());
    }

    @Test
    void allDataSourcesProvideTheSameJdbcAndTransactionServices() throws Throwable {
        try (BootApplication application = this.exampleBoot().start()) {
            AppContext context = application.getAppContext();
            assertEquals(3, context.findBindingRegister(DataSource.class).size());
            ConnectionProvider connections = context.getInstance(ConnectionProvider.class);
            HintsSet hints = new HintsSet();
            assertNull(connections.findConnection("business", hints));
            assertNull(connections.findConnection("unknown", hints));
            for (String name : new String[] { "", "ds1", "ds2" }) {
                DataSource source;
                JdbcTemplate jdbc;
                TransactionTemplate transactions;
                Session session;
                if (name.isEmpty()) {
                    source = context.getInstance(DataSource.class);
                    jdbc = context.getInstance(JdbcTemplate.class);
                    transactions = context.getInstance(TransactionTemplate.class);
                    session = context.getInstance(Session.class);
                } else {
                    source = context.findBindingBean(name, DataSource.class);
                    jdbc = context.findBindingBean(name, JdbcTemplate.class);
                    transactions = context.findBindingBean(name, TransactionTemplate.class);
                    session = context.findBindingBean(name, Session.class);
                }
                assertSame(source, jdbc.getDataSource(), name);
                assertNotNull(session, name);
                try (var expected = source.getConnection(); var actual = connections.findConnection(name, hints)) {
                    assertEquals(expected.getMetaData().getURL(), actual.getMetaData().getURL(), name);
                    if (name.isEmpty()) {
                        try (var unnamed = connections.findConnection(null, hints); var blank = connections.findConnection("  ", hints)) {
                            assertEquals(expected.getMetaData().getURL(), unnamed.getMetaData().getURL());
                            assertEquals(expected.getMetaData().getURL(), blank.getMetaData().getURL());
                        }
                    }
                }
                jdbc.execute("CREATE TABLE example_transaction (id INT PRIMARY KEY, balance INT)");
                jdbc.execute("INSERT INTO example_transaction VALUES (1, 100)");

                transactions.execute(status -> {
                    jdbc.execute("UPDATE example_transaction SET balance = 105 WHERE id = 1");
                    assertEquals(100, this.committedBalance(source));
                    return null;
                });
                assertEquals(105, this.committedBalance(source));

                SQLException failure = new SQLException("Business operation failed");
                assertSame(failure, assertThrows(SQLException.class, () -> transactions.execute(status -> {
                    jdbc.execute("UPDATE example_transaction SET balance = 120 WHERE id = 1");
                    throw failure;
                })));
                assertEquals(105, this.committedBalance(source));
            }
        }
    }

    private int committedBalance(DataSource source) throws SQLException {
        try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT balance FROM example_transaction WHERE id = 1")) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }

    @Test
    void metadataCommitsIndependentlyOfTheHostTransaction() throws Throwable {
        try (BootApplication application = this.exampleBoot().start()) {
            AppContext context = application.getAppContext();
            AdminService admin = context.getInstance(Dataway.class).getAdminService();
            TransactionTemplate transactions = context.getInstance(TransactionTemplate.class);
            DataSource source = context.getInstance(DataSource.class);
            JdbcTemplate jdbc = context.getInstance(JdbcTemplate.class);
            jdbc.execute("CREATE TABLE example_transaction (id INT PRIMARY KEY, balance INT)");
            jdbc.execute("INSERT INTO example_transaction VALUES (1, 100)");
            long originalVersion = admin.getVersionById("example-person");
            transactions.execute(status -> {
                jdbc.execute("UPDATE example_transaction SET balance = 105 WHERE id = 1");
                var draft = admin.getDraftByApi("example-person");
                draft.setDescription("Saved with an independent metadata transaction");
                var saved = admin.save(draft, originalVersion);
                admin.publish(saved.getApiID(), saved.getRevision());
                assertEquals(2, admin.getHistoryByApi("example-person").size());
                try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_release WHERE pub_api_id = 'example-person'")) {
                    assertTrue(rows.next());
                    assertEquals(2, rows.getInt(1));
                }
                status.setRollback();
                return null;
            });
            assertEquals(100, this.committedBalance(source));
            assertEquals(originalVersion + 2, admin.getVersionById("example-person"));
            assertEquals(2, admin.getHistoryByApi("example-person").size());
            int port = context.getInstance(WebServer.class).getPort();
            try (HttpClient client = new HttpClient("http://127.0.0.1:" + port)) {
                assertEquals(200, client.login("admin").status);
                JsonNode detail = this.result(client.get("/admin/api/api-detail?id=example-person")).path("result");
                assertEquals("Saved with an independent metadata transaction", detail.path("apiComment").asText(), detail.toString());
                assertEquals("Alice", this.structuredResult(client.json("/api/person", Map.of("id", 1))).path("name").asText());
            }
        }
    }

    @Test
    void repeatedInitializationPreservesEditedDraftsAndDisabledApis() throws Exception {
        try (BootApplication application = this.exampleBoot().start()) {
            AppContext context = application.getAppContext();
            AdminService admin = context.getInstance(Dataway.class).getAdminService();
            var draft = admin.getDraftByApi("example-person");
            draft.setDescription("Edited by the application user");
            draft.setScript("return 'edited';");
            var saved = admin.save(draft, admin.getVersionById(draft.getId()));
            admin.disableApi("example-orders", admin.getVersionById("example-orders"));

            context.getInstance(ExampleApiService.class).initialize();

            assertEquals(4, admin.list().size());
            assertEquals("return 'edited';", admin.getDraftByApi(draft.getId()).getScript());
            assertEquals(saved.getRevision(), admin.getVersionById(draft.getId()));
            assertEquals(1, admin.getHistoryByApi(draft.getId()).size());
            assertFalse(admin.getApiById("example-orders").isEnabled());
            int port = context.getInstance(WebServer.class).getPort();
            try (HttpClient client = new HttpClient("http://127.0.0.1:" + port)) {
                assertEquals(200, client.login("api").status);
                JsonNode person = this.structuredResult(client.json("/api/person", Map.of("id", 1)));
                assertEquals("Alice", person.path("name").asText(), person.toString());
                assertEquals(404, client.json("/api/orders", Map.of("id", 1)).status);
            }
        }
    }

    @Test
    void bootServerAcceptsRealLoginAndPublishedApiRequests() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/hello", "return 'hello';");

            assertEquals(401, client.get("/api/hello").status);
            var login = client.login("api");
            assertEquals(200, login.status, login.text());
            var response = client.get("/api/hello");
            assertEquals(200, response.status, response.text());
            assertEquals("\"hello\"", response.text());
        }
    }
}
