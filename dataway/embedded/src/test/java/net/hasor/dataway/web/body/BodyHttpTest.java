/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.config.ServiceTestSupport;
import net.hasor.dataway.web.support.HttpTestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.METHOD;
import static net.hasor.dataway.dal.FieldDef.SCRIPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class BodyHttpTest extends ServiceTestSupport {
    private HttpTestServer server;

    @BeforeEach
    void startHttpHost() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("echo", "1", 1), "release", "1", 1);
        release.put(METHOD, "POST");
        release.put(SCRIPT, """
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                return [${name}, ${tag}, ${flag}, web.jsonBody()];
                """);
        this.publishRoute(release);
        this.config.defaultResultHandler("raw").identityProvider(r -> UserIdentity.authenticated("caller", Map.of()));
        this.server = new HttpTestServer("/api", this.config.createDataway().getApiHandler());
    }

    @AfterEach
    void stopHttpHost() {
        if (this.server != null) {
            this.server.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "UTF-8", "UTF-16LE", "GB18030" })
    void realHttpJsonUsesItsCharsetAndOverridesQueryValues(String encoding) throws Exception {
        Map<String, Object> body = Map.of("name", "中文", "tag", List.of("a", "b"), "flag", "");
        byte[] bytes = JsonUtils.writeValueAsString(body).getBytes(Charset.forName(encoding));
        HttpResponse<String> response = this.server.send("POST", "/api/echo?name=query&tag=old", "Application/JSON; Charset=\"" + encoding + "\"", bytes);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(List.of("中文", List.of("a", "b"), "", body), JsonUtils.readValue(response.body(), List.class));
    }

    @Test
    void realHttpFormsPreserveRepeatedFieldsAndKeepQueryValuesOutOfTheBody() throws Exception {
        String body = "name=%E4%B8%AD%E6%96%87&tag=first&tag=second&tag=third&flag&formula=a%2Bb+c%3Dd&empty=";
        HttpResponse<String> response = this.server.send("POST", "/api/echo?name=query&queryOnly=hidden", "application/x-www-form-urlencoded; charset=UTF-8", body.getBytes(StandardCharsets.US_ASCII));
        assertEquals(200, response.statusCode(), response.body());
        Map<String, Object> parsed = Map.of("name", "中文", "tag", List.of("first", "second", "third"), "flag", "", "formula", "a+b c=d", "empty", "");
        assertEquals(List.of("中文", List.of("first", "second", "third"), "", parsed), JsonUtils.readValue(response.body(), List.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "application/json", "application/x-www-form-urlencoded" })
    void emptyHttpBodiesAreValidEvenWithoutAContentType(String contentType) throws Exception {
        HttpResponse<String> response = this.server.send("POST", "/api/echo", contentType, new byte[0]);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("[null,null,null,{}]", response.body());
    }

    @ParameterizedTest
    @ValueSource(strings = { "[]", "null", "{broken", "\"scalar\"" })
    void invalidJsonBodiesReturn400BeforeLookingUpTheApi(String json) throws Exception {
        HttpResponse<String> response = this.server.send("POST", "/api/echo", "application/json", json.getBytes(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode());
        verify(this.access, never()).listObjects(eq(EntityType.RELEASE), anyMap());
    }

    @Test
    void invalidFormEscapesReturn400BeforeLookingUpTheApi() throws Exception {
        HttpResponse<String> response = this.server.send("POST", "/api/echo", "application/x-www-form-urlencoded", "name=%GG".getBytes(StandardCharsets.US_ASCII));
        assertEquals(400, response.statusCode());
        verify(this.access, never()).listObjects(eq(EntityType.RELEASE), anyMap());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "text/plain", "application/octet-stream", "application/xml", "application/json; charset=unsupported" })
    void unsupportedOrMissingContentTypesReturn415ForNonEmptyBodies(String contentType) throws Exception {
        HttpResponse<String> response = this.server.send("POST", "/api/echo", contentType, "data".getBytes(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode());
        verify(this.access, never()).listObjects(eq(EntityType.RELEASE), anyMap());
    }

    @Test
    void aHostWithoutMultipartSupportRejectsAnOtherwiseValidMultipartRequest() throws Exception {
        String multipart = "--upload\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\nvalue\r\n--upload--\r\n";
        HttpResponse<String> response = this.server.send("POST", "/api/echo", "multipart/form-data; boundary=upload", multipart.getBytes(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode());
        assertTrue(response.body().contains("request adapter"));
        verify(this.access, never()).listObjects(eq(EntityType.RELEASE), anyMap());
    }
}
