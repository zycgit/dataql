/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.configuration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.spring.example.service.ExampleApiService;
import net.hasor.dataway.spring.testcase.ExampleServer;
import net.hasor.dataway.spring.testcase.HttpClient;
import net.hasor.dataway.spring.testcase.HttpResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class BootHttpTest {
    @Test
    void standaloneExampleBootsFromNativeConfiguration() throws Throwable {
        try (ExampleServer application = new ExampleServer()) {
            var context = application.context();
            this.assertTables(context.getBean(DataSource.class), Set.of("INTERFACE_INFO", "INTERFACE_RELEASE"));
            this.assertTables(application.source("ds1"), Set.of("EXAMPLE_PEOPLE"));
            this.assertTables(application.source("ds2"), Set.of("EXAMPLE_ORDERS"));
            try (HttpClient client = new HttpClient(application.baseUrl())) {
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

    private void assertTables(DataSource source, Set<String> expected) throws Throwable {
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
    void repeatedInitializationPreservesEditedDraftsAndDisabledApis() throws Throwable {
        try (ExampleServer application = new ExampleServer()) {
            var context = application.context();
            AdminService admin = context.getBean(Dataway.class).getAdminService();
            var draft = admin.getDraftByApi("example-person");
            draft.setDescription("Edited by the application user");
            draft.setScript("return 'edited';");
            var saved = admin.save(draft, admin.getVersionById(draft.getId()));
            admin.disableApi("example-orders", admin.getVersionById("example-orders"));

            context.getBean(ExampleApiService.class).initialize();

            assertEquals(4, admin.list().size());
            assertEquals("return 'edited';", admin.getDraftByApi(draft.getId()).getScript());
            assertEquals(saved.getRevision(), admin.getVersionById(draft.getId()));
            assertEquals(1, admin.getHistoryByApi(draft.getId()).size());
            assertFalse(admin.getApiById("example-orders").isEnabled());
            try (HttpClient client = new HttpClient(application.baseUrl())) {
                assertEquals(200, client.login("api").status);
                JsonNode person = this.structuredResult(client.json("/api/person", Map.of("id", 1)));
                assertEquals("Alice", person.path("name").asText(), person.toString());
                assertEquals(404, client.json("/api/orders", Map.of("id", 1)).status);
            }
        }
    }

    @Test
    void connectionProviderResolvesDefaultAndNamedSourcesFromTheHost() throws Throwable {
        try (ExampleServer application = new ExampleServer()) {
            var context = application.context();
            ConnectionProvider connections = context.getBean(ConnectionProvider.class);
            HintsSet hints = new HintsSet();
            for (String name : new String[] { "", "ds1", "ds2" }) {
                DataSource source = name.isEmpty() ? context.getBean(DataSource.class) : application.source(name);
                try (var expected = source.getConnection(); var actual = connections.findConnection(name, hints)) {
                    assertEquals(expected.getMetaData().getURL(), actual.getMetaData().getURL());
                    if (name.isEmpty()) {
                        try (var unnamed = connections.findConnection(null, hints); var blank = connections.findConnection("  ", hints)) {
                            assertEquals(expected.getMetaData().getURL(), unnamed.getMetaData().getURL());
                            assertEquals(expected.getMetaData().getURL(), blank.getMetaData().getURL());
                        }
                    }
                }
            }
            assertNull(connections.findConnection("missing", hints));
        }
    }

    @Test
    void metadataCommitsIndependentlyOfTheHostTransaction() throws Throwable {
        try (ExampleServer application = new ExampleServer()) {
            var context = application.context();
            AdminService admin = context.getBean(Dataway.class).getAdminService();
            var transactions = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.execute("CREATE TABLE example_transaction (id INT PRIMARY KEY, balance INT)");
            jdbc.execute("INSERT INTO example_transaction VALUES (1, 100)");
            long version = admin.getVersionById("example-person");
            transactions.execute(status -> {
                jdbc.update("UPDATE example_transaction SET balance = 105 WHERE id = 1");
                var draft = admin.getDraftByApi("example-person");
                draft.setDescription("Saved with an independent metadata transaction");
                var saved = admin.save(draft, version);
                admin.publish(saved.getApiID(), saved.getRevision());
                assertEquals(2, admin.getHistoryByApi("example-person").size());
                status.setRollbackOnly();
                return null;
            });
            assertEquals(100, jdbc.queryForObject("SELECT balance FROM example_transaction WHERE id = 1", Integer.class));
            assertEquals(version + 2, admin.getVersionById("example-person"));
            assertEquals(2, admin.getHistoryByApi("example-person").size());
            try (HttpClient client = new HttpClient(application.baseUrl())) {
                assertEquals(200, client.login("admin").status);
                JsonNode detail = this.result(client.get("/admin/api/api-detail?id=example-person"));
                assertEquals("Saved with an independent metadata transaction", detail.path("result").path("apiComment").asText());
                assertEquals("Alice", this.structuredResult(client.json("/api/person", Map.of("id", 1))).path("name").asText());
            }
        }
    }
}
