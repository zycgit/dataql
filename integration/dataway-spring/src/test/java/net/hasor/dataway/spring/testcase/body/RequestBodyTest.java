/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.body;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.spring.testcase.H2Database;
import net.hasor.dataway.spring.testcase.HttpClient;
import net.hasor.dataway.spring.testcase.TestApplication;
import net.hasor.dataway.spring.testcase.TestSettings;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestBodyTest {
    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH" })
    void realUrlEncodedFormsKeepRepeatedFieldsAndOverrideQueryValues(String method) throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), method, "/form", "return [${name}, ${tag}, ${empty}, ${encoded}, ${queryOnly}];");
            assertEquals(200, client.login("api").status);
            var form = new FormBody.Builder().add("name", "表单").add("tag", "one").add("tag", "two").add("empty", "").add("encoded", "a+b & c=1").build();
            var response = client.send(method, "/api/form?name=query&queryOnly=retained", form);
            assertEquals(200, response.status, response.text());
            assertEquals(List.of("表单", List.of("one", "two"), "", "a+b & c=1", "retained"), JsonUtils.readValue(response.text(), List.class));
        }
    }

    @Test
    void jsonCharsetAndEmptyBodiesAreDecodedOverHttp() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/json", "return ${name};");
            assertEquals(200, client.login("api").status);
            byte[] bytes = "{\"name\":\"café\"}".getBytes(StandardCharsets.ISO_8859_1);
            var body = RequestBody.create(bytes, MediaType.get("application/json; charset=ISO-8859-1"));
            var response = client.send("POST", "/api/json", body);
            assertEquals(200, response.status, response.text());
            assertEquals("café", JsonUtils.readValue(response.text(), String.class));
            var emptyJson = client.send("POST", "/api/json?name=empty-json", RequestBody.create("", MediaType.get("application/json")));
            assertEquals("\"empty-json\"", emptyJson.text());
            var noType = client.send("POST", "/api/json?name=empty", RequestBody.create(new byte[0], null));
            assertEquals(200, noType.status, noType.text());
            assertEquals("\"empty\"", noType.text());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "{broken", "[]", "null", "1", "\"text\"" })
    void invalidOrNonObjectJsonReturnsBadRequest(String json) throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/json", "return 1;");
            assertEquals(200, client.login("api").status);
            var response = client.send("POST", "/api/json", RequestBody.create(json, MediaType.get("application/json")));
            assertEquals(400, response.status, response.text());
        }
    }

    @Test
    void unsupportedOrMissingContentTypeIsRejectedBeforeScriptExecution() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/typed", "return 1;");
            assertEquals(200, client.login("api").status);
            for (String type : new String[] { "text/plain", "application/xml", "application/octet-stream" }) {
                var result = client.send("POST", "/api/typed", RequestBody.create("body", MediaType.get(type)));
                assertEquals(415, result.status, result.text());
            }
            var missing = client.send("POST", "/api/typed", RequestBody.create(new byte[] { 1 }, null));
            assertEquals(415, missing.status, missing.text());
            var badCharset = client.send("POST", "/api/typed", RequestBody.create(new byte[] { 1 }, null), "Content-Type", "application/json; charset=not-a-charset");
            assertEquals(415, badCharset.status, badCharset.text());
        }
    }
}
