/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.admin;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.solon.testcase.*;
import okhttp3.FormBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class LoginTest {
    @Test
    void mvcAuthenticationStopsUnauthenticatedRequestsBeforeDataway() throws Throwable {
        AtomicInteger checks = new AtomicInteger();
        var config = TestSettings.configuration().authorizationCheck((identity, operation) -> {
            checks.incrementAndGet();
            assertEquals("api", identity.identityId());
            assertEquals(Map.of("tenant", "example"), identity.attributes());
            return identity.checkOperation(operation);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/private", "return 'private';");
            for (String path : new String[] { "/api/private", "/admin/api/api-list", "/admin/", "/docs/openapi.json" }) {
                var response = client.get(path);
                assertEquals(401, response.status, response.text());
                assertEquals("Login required", response.json().get("message"));
                assertEquals("applied", response.headers.get("X-Host-Interceptor"));
            }
            var currentUser = client.json("/session/me", Map.of());
            assertEquals(401, currentUser.status, currentUser.text());
            assertEquals("Login required", currentUser.json().get("message"));
            assertEquals("applied", currentUser.headers.get("X-Host-Interceptor"));
            assertEquals(0, checks.get());
            assertEquals(200, client.login("api").status);
            assertEquals(200, client.get("/api/private").status);
            assertEquals(1, checks.get());
        }
    }

    @Test
    void loginControllerOwnsLoginLogoutAndCurrentUserRoutes() throws Throwable {
        try (TestApplication app = new TestApplication((DatawayConfig) null, null, new Properties()); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("reader").status);
            assertEquals(404, client.get("/session/unknown").status);
            assertEquals(405, client.get("/session/login").status);
            assertEquals(200, client.login("reader").status);
            var currentUser = client.json("/session/me", Map.of());
            assertEquals(200, currentUser.status, currentUser.text());
            assertEquals(true, currentUser.json().get("authenticated"));
            assertEquals(Map.of("tenant", "example"), currentUser.json().get("attributes"));
            assertEquals(405, client.get("/session/logout").status);
            assertEquals(200, client.json("/session/me", Map.of()).status);
            assertEquals(405, client.get("/session/me").status);
            var logout = client.json("/session/logout", Map.of());
            assertEquals(200, logout.status, logout.text());
            assertEquals(true, logout.json().get("success"));
            assertTrue(logout.headers.get("Set-Cookie").contains("Max-Age=0"));
            assertEquals(401, client.json("/session/me", Map.of()).status);
        }
    }

    @Test
    void credentialsBrowserIsolationAndStatelessLogoutAreVerifiedOverHttp() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient browser = new HttpClient(app.baseUrl()); HttpClient stranger = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/private", "return 'private';");
            assertEquals(401, browser.get("/api/private").status);
            assertEquals(401, browser.get("/admin/api/api-list").status);
            assertEquals(401, browser.get("/admin/").status);
            var incorrect = new FormBody.Builder().add("username", "admin").add("password", "wrong").build();
            assertEquals(401, browser.send("POST", "/session/login", incorrect).status);
            assertEquals(401, browser.login("unknown").status);
            HttpResult login = browser.login("admin");
            assertEquals(200, login.status, login.text());
            String cookie = login.headers.get("Set-Cookie");
            assertNotNull(cookie);
            assertTrue(cookie.toLowerCase().contains("httponly"));
            assertTrue(cookie.contains("SameSite=Strict"));
            assertTrue(cookie.contains("Path=/"));
            String credential = cookie.split(";", 2)[0];
            assertEquals(200, browser.json("/session/me", Map.of()).status);
            assertEquals(Map.of("tenant", "example"), browser.json("/session/me", Map.of()).json().get("attributes"));
            assertEquals(200, browser.get("/api/private").status);
            assertEquals(401, stranger.get("/api/private").status);
            assertEquals(401, stranger.send("GET", "/api/private", null, "Cookie", "EXAMPLE_TOKEN=forged").status);
            assertEquals(200, browser.json("/session/logout", Map.of()).status);
            assertEquals(401, browser.get("/api/private").status);
            // Stateless logout clears the browser cookie; an issued JWT remains valid until expiration.
            assertEquals(200, stranger.send("GET", "/api/private", null, "Cookie", credential).status);
            assertEquals(401, browser.get("/admin/api/api-list").status);
        }
    }

    @ParameterizedTest
    @CsvSource({ "api,401,401,401", "reader,200,200,401", "admin,200,200,200" })
    void hostLoginResolvesTheThreePermissionPresets(String account, int listStatus, int uiStatus, int debugStatus) throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/hello", "return 'hello';");
            assertEquals(200, client.login(account).status);
            assertEquals(200, client.get("/api/hello").status);
            assertEquals(listStatus, client.get("/admin/api/api-list").status);
            assertEquals(uiStatus, client.get("/admin/").status);
            Map<String, Object> draft = Map.of("id", "-1", "select", "GET", "apiPath", "/preview", "codeType", "DataQL", "codeValue", "return 7;");
            var debug = client.json("/admin/api/perform", draft);
            assertEquals(debugStatus, debug.status, debug.text());
            if (debugStatus == 200) {
                assertEquals(true, debug.json().get("success"));
                assertEquals(7, debug.json().get("value"));
            }
        }
    }
}
