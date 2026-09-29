/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.api;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HostSqlTestApplication;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.HttpResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

class HostSqlTransactionTest {
    @Test
    void sqlConnectionsParticipateInHostCommitAndRollback() throws Throwable {
        try (H2Database database = new H2Database(); HostSqlTestApplication app = new HostSqlTestApplication(database); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            Map<String, Object> draft = this.draft("SQL", "/transaction", "UPDATE example_people SET balance = balance + #{amount} WHERE id = #{id}", Map.of("id", 1, "amount", 0, "rollback", false));
            draft.put("optionInfo", Map.of("resultStructure", true));
            this.publish(client, draft);
            this.publish(client, this.draft("SQL", "/balance", "SELECT balance FROM example_people WHERE id = #{id}", Map.of("id", 1)));
            assertEquals(200, client.login("api").status);

            JsonNode committed = this.result(client.json("/api/transaction", Map.of("id", 1, "amount", 5)));
            assertTrue(committed.path("success").asBoolean(), committed.toString());
            assertEquals(1, committed.path("value").asInt());
            assertEquals(105, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());

            JsonNode rolledBack = this.result(client.json("/api/transaction", Map.of("id", 1, "amount", 10, "rollback", true)));
            assertFalse(rolledBack.path("success").asBoolean(true), rolledBack.toString());
            assertTrue(rolledBack.path("message").asText().contains("rolled back"), rolledBack.toString());
            assertEquals(105, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
            assertEquals(200, this.result(client.json("/api/balance", Map.of("id", 2))).asInt());

            assertTrue(this.result(client.json("/api/transaction", Map.of("id", 1, "amount", 1))).path("success").asBoolean());
            assertEquals(106, this.result(client.json("/api/balance", Map.of("id", 1))).asInt());
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

    private void publish(HttpClient client, Map<String, Object> draft) throws Exception {
        JsonNode saved = this.result(client.json("/admin/api/save-api", draft));
        assertTrue(saved.path("success").asBoolean(), saved.toString());
        JsonNode published = this.result(client.json("/admin/api/publish", Map.of("id", saved.path("result").asText(), "version", 1)));
        assertTrue(published.path("success").asBoolean(), published.toString());
    }

}
