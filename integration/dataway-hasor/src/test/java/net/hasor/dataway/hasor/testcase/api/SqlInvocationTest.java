/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.api;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.testcase.H2Database;
import net.hasor.dataway.hasor.testcase.HttpClient;
import net.hasor.dataway.hasor.testcase.HttpResult;
import net.hasor.dataway.hasor.testcase.SqlTestApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class SqlInvocationTest {
    @Test
    void consolePreviewsAndPublishesSqlWithBoundHttpParameters() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient console = new HttpClient(app.baseUrl()); HttpClient caller = new HttpClient(app.baseUrl())) {
            assertEquals(200, console.login("admin").status);
            String sql = "SELECT id AS \"id\", name AS \"name\", balance AS \"balance\" FROM example_people WHERE id = #{id}";
            Map<String, Object> draft = this.draft("SQL", "/person", sql, Map.of("id", 1));
            JsonNode preview = this.result(console.json("/admin/api/perform", draft));
            assertEquals("Alice", preview.path("name").asText(), preview.toString());
            JsonNode saved = this.result(console.json("/admin/api/save-api", draft));
            assertTrue(saved.path("success").asBoolean(), saved.toString());
            String id = saved.path("result").asText();
            assertEquals(200, caller.login("api").status);
            assertEquals(404, caller.json("/api/person", Map.of("id", 1)).status);
            JsonNode smoke = this.result(console.json("/admin/api/smoke", Map.of("id", id, "version", 1, "requestBody", Map.of("id", 2))));
            assertEquals("Bob", smoke.path("name").asText(), smoke.toString());
            assertTrue(this.result(console.json("/admin/api/publish", Map.of("id", id, "version", 1))).path("success").asBoolean());
            JsonNode person = this.result(caller.json("/api/person?id=1", Map.of("id", 2)));
            assertEquals(2, person.path("id").asInt(), person.toString());
            assertEquals("Bob", person.path("name").asText());
            assertEquals(200, person.path("balance").asInt());
            assertEquals(2, app.count("example_people"));
            JsonNode detail = this.result(console.get("/admin/api/api-detail?id=" + id)).path("result");
            assertEquals(sql, detail.path("codeInfo").path("codeValue").asText());
            assertEquals("SQL", detail.path("codeType").asText());
        }
    }

    private Map<String, Object> draft(String type, String path, String script, Map<String, ?> parameters) {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("id", "-1");
        draft.put("version", 0);
        draft.put("select", "POST");
        draft.put("apiPath", path);
        draft.put("codeType", type);
        draft.put("codeValue", script);
        draft.put("comment", "SQL through the host data source");
        draft.put("requestBody", parameters);
        draft.put("optionInfo", Map.of("resultStructure", false));
        return draft;
    }

    private JsonNode result(HttpResult result) {
        assertEquals(200, result.status, result.text());
        return JsonUtils.readTree(result.text());
    }

    @Test
    void dataqlFragmentsUseTheNamedHostDataSource() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            String script = """
                    hint FRAGMENT_SQL_DATA_SOURCE = "ds1"
                    var findPerson = @@selectSql(id)<%
                        SELECT name FROM example_people WHERE id = #{id}
                    %>;
                    return findPerson(${id});
                    """;
            this.publish(client, this.draft("DataQL", "/fragment", script, Map.of("id", 1)));
            assertEquals("Alice", this.result(client.json("/api/fragment", Map.of("id", 1))).asText());
            assertEquals("Bob", this.result(client.json("/api/fragment", Map.of("id", 2))).asText());
        }
    }

    private void publish(HttpClient client, Map<String, Object> draft) throws Exception {
        JsonNode saved = this.result(client.json("/admin/api/save-api", draft));
        assertTrue(saved.path("success").asBoolean(), saved.toString());
        JsonNode published = this.result(client.json("/admin/api/publish", Map.of("id", saved.path("result").asText(), "version", 1)));
        assertTrue(published.path("success").asBoolean(), published.toString());
    }

    @Test
    void sqlUpdatesTheHostDatabaseAndTreatsInjectionTextAsAValue() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            this.publish(client, this.draft("SQL", "/rename", "UPDATE example_people SET name = #{name} WHERE id = #{id}", Map.of("id", 1, "name", "new")));
            this.publish(client, this.draft("SQL", "/find", "SELECT name FROM example_people WHERE id = #{id}", Map.of("id", 1)));
            String name = "小明'; DROP TABLE example_people; --";
            assertEquals(1, this.result(client.json("/api/rename", Map.of("id", 1, "name", name))).asInt());
            assertEquals(name, this.result(client.json("/api/find", Map.of("id", 1))).asText());
            assertEquals("Bob", this.result(client.json("/api/find", Map.of("id", 2))).asText());
            assertEquals(2, app.count("example_people"));
        }
    }

    @Test
    void wrappedParametersKeepDefaultStructuredResultsForSqlQueries() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            Map<String, Object> draft = this.draft("SQL", "/wrapped", "SELECT name FROM example_people WHERE id = #{input.id}", Map.of("id", 1));
            draft.put("optionInfo", Map.of("wrapAllParameters", true, "wrapParameterName", "input"));
            this.publish(client, draft);
            JsonNode result = this.result(client.json("/api/wrapped", Map.of("id", 2)));
            assertTrue(result.path("success").asBoolean(), result.toString());
            assertEquals("Bob", result.path("value").asText(), result.toString());
        }
    }

    @Test
    void databaseErrorsProduceFailureResultsAndDoNotBreakLaterQueries() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            Map<String, Object> invalid = this.draft("SQL", "/broken", "SELECT name FROM missing_business_table WHERE id = #{id}", Map.of("id", 1));
            invalid.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, invalid);
            JsonNode failed = this.result(client.json("/api/broken", Map.of("id", 1)));
            assertFalse(failed.path("success").asBoolean(true), failed.toString());
            assertFalse(failed.path("message").asText().isBlank(), failed.toString());
            this.publish(client, this.draft("SQL", "/healthy", "SELECT name FROM example_people WHERE id = #{id}", Map.of("id", 1)));
            assertEquals("Alice", this.result(client.json("/api/healthy", Map.of("id", 1))).asText());
            assertEquals(2, app.count("example_people"));
        }
    }

    @ParameterizedTest
    @CsvSource({ "'', example_people, balance, 1, 100", "ds1, example_people, balance, 1, 100", "ds2, example_orders, amount, 101, 89.90" })
    void dataqlTransactionsCommitAndRollBackSqlFragments(String sourceName, String table, String column, int id, double initialValue) throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            String script = """
                    hint FRAGMENT_SQL_DATA_SOURCE = "%s"
                    import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                    var changeValue = @@updateSql(id, amount)<%%
                        UPDATE %s SET %s = %s + #{amount} WHERE id = #{id}
                    %%>;
                    var findValue = @@selectSql(id)<%%
                        SELECT %s FROM %s WHERE id = #{id}
                    %%>;
                    var failSql = @@insertSql()<%%
                        INSERT INTO missing_business_table (id) VALUES (1)
                    %%>;
                    return tran.required(() -> {
                        run changeValue(${id}, ${amount});
                        run changeValue(${id}, ${amount});
                        if (${rollback}) {
                            run failSql();
                        }
                        return findValue(${id});
                    });
                    """.formatted(sourceName, table, column, column, column, table);
            Map<String, Object> draft = this.draft("DataQL", "/transaction", script, Map.of("id", id, "amount", 0, "rollback", false));
            draft.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, draft);
            this.publish(client, this.draft("DataQL", "/value", this.readValue(sourceName, table, column), Map.of("id", id)));
            if (!sourceName.equals("ds2")) {
                String otherSource = sourceName.isEmpty() ? "ds1" : "";
                this.publish(client, this.draft("DataQL", "/other-value", this.readValue(otherSource, table, column), Map.of("id", id)));
            }
            assertEquals(200, client.login("api").status);

            JsonNode committed = this.result(client.json("/api/transaction", Map.of("id", id, "amount", 5, "rollback", false)));
            assertTrue(committed.path("success").asBoolean(), committed.toString());
            assertEquals(initialValue + 10, committed.path("value").asDouble(), 0.001);
            assertEquals(initialValue + 10, this.result(client.json("/api/value", Map.of("id", id))).asDouble(), 0.001);

            JsonNode rolledBack = this.result(client.json("/api/transaction", Map.of("id", id, "amount", 10, "rollback", true)));
            assertFalse(rolledBack.path("success").asBoolean(true), rolledBack.toString());
            assertFalse(rolledBack.path("message").asText().isBlank(), rolledBack.toString());
            assertEquals(initialValue + 10, this.result(client.json("/api/value", Map.of("id", id))).asDouble(), 0.001);

            assertTrue(this.result(client.json("/api/transaction", Map.of("id", id, "amount", 1, "rollback", false))).path("success").asBoolean());
            assertEquals(initialValue + 12, this.result(client.json("/api/value", Map.of("id", id))).asDouble(), 0.001);
            if (!sourceName.equals("ds2")) {
                assertEquals(initialValue, this.result(client.json("/api/other-value", Map.of("id", id))).asDouble(), 0.001);
            }
        }
    }

    private String readValue(String sourceName, String table, String column) {
        return """
                hint FRAGMENT_SQL_DATA_SOURCE = "%s"
                var findValue = @@selectSql(id)<%%
                    SELECT %s FROM %s WHERE id = #{id}
                %%>;
                return findValue(${id});
                """.formatted(sourceName, column, table);
    }

    @Test
    void requiresNewCommitsIndependentlyOfEarlierOuterChanges() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            String script = """
                    import 'net.hasor.dataql.sqlproc.execute.transaction.TransactionUdfSource' as tran;
                    var changeBalance = @@updateSql(id, amount)<%
                        UPDATE example_people SET balance = balance + #{amount} WHERE id = #{id}
                    %>;
                    var failSql = @@insertSql()<%
                        INSERT INTO missing_business_table (id) VALUES (1)
                    %>;
                    return tran.required(() -> {
                        run changeBalance(1, 10);
                        var committed = tran.requiresNew(() -> {
                            run changeBalance(2, 5);
                            return true;
                        });
                        run failSql();
                        return committed;
                    });
                    """;
            Map<String, Object> draft = this.draft("DataQL", "/transaction", script, Map.of());
            draft.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, draft);
            this.publish(client, this.draft("SQL", "/balance", "SELECT balance FROM example_people WHERE id = #{id}", Map.of("id", 1)));
            assertEquals(200, client.login("api").status);

            JsonNode failed = this.result(client.json("/api/transaction", Map.of()));
            assertFalse(failed.path("success").asBoolean(true), failed.toString());
            assertFalse(failed.path("message").asText().isBlank(), failed.toString());
            assertEquals(100, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
            assertEquals(205, this.result(client.json("/api/balance", Map.of("id", 2))).asInt());
        }
    }

    @Test
    void scriptWithoutTransactionKeepsCompletedSqlWhenLaterSqlFails() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            String script = """
                    var changeBalance = @@updateSql()<%
                        UPDATE example_people SET balance = balance + 5 WHERE id = 1
                    %>;
                    var failSql = @@insertSql()<%
                        INSERT INTO missing_business_table (id) VALUES (1)
                    %>;
                    run changeBalance();
                    return failSql();
                    """;
            Map<String, Object> draft = this.draft("DataQL", "/without-transaction", script, Map.of());
            draft.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, draft);
            this.publish(client, this.draft("SQL", "/balance", "SELECT balance FROM example_people WHERE id = #{id}", Map.of("id", 1)));
            assertEquals(200, client.login("api").status);

            JsonNode failed = this.result(client.json("/api/without-transaction", Map.of()));
            assertFalse(failed.path("success").asBoolean(true), failed.toString());
            assertFalse(failed.path("message").asText().isBlank(), failed.toString());
            assertEquals(105, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
        }
    }

    @Test
    void publishedTransferExampleRollsBackWhenTheTargetAccountIsMissing() throws Throwable {
        try (H2Database database = new H2Database(); SqlTestApplication app = new SqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            String script;
            try (var input = this.getClass().getResourceAsStream("/example/dataway/transfer.dql")) {
                assertNotNull(input);
                script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            Map<String, Object> draft = this.draft("DataQL", "/transfer", script, Map.of("fromId", 1, "toId", 2, "amount", 5));
            draft.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, draft);
            this.publish(client, this.draft("DataQL", "/balance", this.readValue("ds1", "example_people", "balance"), Map.of("id", 1)));
            assertEquals(200, client.login("api").status);

            assertTrue(this.result(client.json("/api/transfer", Map.of("fromId", 1, "toId", 2, "amount", 5))).path("success").asBoolean());
            assertEquals(95, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
            assertEquals(205, this.result(client.json("/api/balance", Map.of("id", 2))).asInt());
            JsonNode failure = this.result(client.json("/api/transfer", Map.of("fromId", 1, "toId", 999, "amount", 10)));
            assertFalse(failure.path("success").asBoolean(true), failure.toString());
            assertEquals(95, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
            assertEquals(205, this.result(client.json("/api/balance", Map.of("id", 2))).asInt());
        }
    }

}
