/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.api;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.hasor.testcase.*;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class InvocationTest {
    @Test
    void jsonQueryHeadersCookiesAndInterceptorsReachTheActualScript() throws Throwable {
        List<String> calls = new CopyOnWriteArrayList<>();
        var config = TestSettings.configuration().apiInterceptor((context, chain) -> {
            calls.add(context.identity().identityId());
            assertEquals(Map.of("tenant", "example"), context.identity().attributes());
            return chain.proceed(context);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/echo", """
                    import 'net.hasor.dataway.function.WebUdfSource' as web;
                    run web.setHeader('X-Reply', 'first');
                    run web.addHeader('X-Reply', 'second');
                    run web.setCookie('preview', 'ok');
                    return [${name}, ${tag}, web.headerArray('X-Multi'), web.cookie('EXAMPLE_TOKEN')];
                    """);
            var login = client.login("api");
            assertEquals(200, login.status, login.text());
            String token = login.headers.get("Set-Cookie").split(";", 2)[0].substring("EXAMPLE_TOKEN=".length());
            RequestBody body = RequestBody.create("{\"name\":\"body\"}", MediaType.get("application/json"));
            HttpResult response = client.send("POST", "/api/echo?name=query&tag=one&tag=two", body, "X-Multi", "one", "x-multi", "two");
            assertEquals(200, response.status, response.text());
            assertEquals(List.of("body", List.of("one", "two"), List.of("one", "two"), token), JsonUtils.readValue(response.text(), List.class));
            assertEquals(List.of("first", "second"), response.headers.values("X-Reply"));
            assertTrue(response.headers.values("Set-Cookie").stream().anyMatch(cookie -> cookie.startsWith("preview=ok")));
            assertEquals("applied", response.headers.get("X-Host-Interceptor"));
            assertEquals(List.of("api"), calls);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" })
    void publishedRoutesPreserveHttpMethods(String method) throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), method, "/method", "return '" + method + "';");
            assertEquals(200, client.login("api").status);
            RequestBody body = List.of("POST", "PUT", "PATCH").contains(method) ? RequestBody.create("{}", MediaType.get("application/json")) : null;
            var response = client.send(method, "/api/method", body);
            assertEquals(200, response.status, response.text());
            assertEquals(method.equals("HEAD") ? "" : "\"" + method + "\"", response.text());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD" })
    void binaryStreamsKeepTheirBytesContentTypeAndHeadSemantics(String method) throws Throwable {
        var config = TestSettings.configuration().apiInterceptor((context, chain) -> {
            return ResultInfoUtils.convertToResultInfo("application/octet-stream", new ByteArrayInputStream(new byte[] { 0, 1, -1 }));
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), method, "/binary", "return 1;");
            assertEquals(200, client.login("api").status);
            var response = client.send(method, "/api/binary", null);
            assertEquals(200, response.status, response.text());
            assertEquals("application/octet-stream", response.headers.get("Content-Type").split(";")[0]);
            assertArrayEquals(method.equals("HEAD") ? new byte[0] : new byte[] { 0, 1, -1 }, response.bytes);
        }
    }

    @Test
    void exceptionsReachTheHostHandlerAndUnmatchedUrlsStayOutsideDataway() throws Throwable {
        var config = TestSettings.configuration().authorizationCheck((identity, operation) -> {
            throw new DatawayException(418, "host-handled");
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/failure", "return 1;");
            assertEquals(200, client.login("api").status);
            var response = client.get("/api/failure");
            assertEquals(418, response.status, response.text());
            assertEquals("host-handled", response.json().get("message"));
            assertEquals(404, client.get("/outside").status);
            assertEquals(404, client.get("/api-other/failure").status);
        }
    }
}
