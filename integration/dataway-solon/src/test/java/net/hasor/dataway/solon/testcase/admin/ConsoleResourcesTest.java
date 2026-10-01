/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.admin;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.TestApplication;
import net.hasor.dataway.solon.testcase.TestSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleResourcesTest {
    @Test
    void consoleRedirectHtmlAndBuiltAssetsAreServedByTheRealContainer() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("reader").status);
            var redirect = client.get("/admin");
            assertEquals(308, redirect.status, redirect.text());
            assertEquals("admin/", redirect.headers.get("Location"));
            var html = client.get("/admin/");
            assertEquals(200, html.status, html.text());
            assertTrue(html.headers.get("Content-Type").startsWith("text/html"));
            assertEquals("nosniff", html.headers.get("X-Content-Type-Options"));
            assertNotNull(html.headers.get("Content-Security-Policy"));
            var matcher = Pattern.compile("(?:src|href)=\"([^\"]+\\.(?:js|css))\"").matcher(html.text());
            int assets = 0;
            while (matcher.find()) {
                String path = matcher.group(1);
                if (path.startsWith("./")) {
                    path = path.substring(2);
                }
                var asset = client.get("/admin/" + path);
                assertEquals(200, asset.status, path + ": " + asset.text());
                assertTrue(asset.bytes.length > 0);
                assertEquals("no-cache", asset.headers.get("Cache-Control"));
                assertTrue(asset.headers.get("Content-Type").startsWith(path.endsWith(".js") ? "text/javascript" : "text/css"));
                assets++;
            }
            assertTrue(assets >= 2, html.text());
            var head = client.send("HEAD", "/admin/", null);
            assertEquals(200, head.status);
            assertEquals(0, head.bytes.length);
            assertEquals(404, client.get("/admin/missing-asset.js").status);
            assertEquals(405, client.json("/admin/", Map.of()).status);
        }
    }

    @ParameterizedTest
    @CsvSource({ "/console,/console/manage,/console/invoke", "/tools/console,/ops/manage,/open/v2", "/tools/console,/tools/manage,/tools/invoke" })
    void initializerConnectsTheUiToConfiguredManagementAndBusinessAddresses(String ui, String admin, String api) throws Throwable {
        Properties settings = TestSettings.enabled();
        settings.setProperty("dataway.admin-ui", ui);
        settings.setProperty("dataway.admin-prefix", admin);
        settings.setProperty("dataway.api-prefix", api);
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/hello", "return 'configured';");
            assertEquals(200, client.login("admin").status);
            var html = client.get(ui + "/");
            assertEquals(200, html.status, html.text());
            assertTrue(html.text().contains("initializer.js"));
            var initializer = client.get(ui + "/initializer.js");
            assertEquals(200, initializer.status, initializer.text());
            Map<?, ?> options = this.initializerOptions(initializer.text());
            URI page = URI.create(app.baseUrl() + ui + "/");
            URI management = page.resolve((String) options.get("adminApi")).resolve("api-list");
            URI invocation = page.resolve((String) options.get("api")).resolve("hello");
            assertEquals(app.baseUrl() + admin + "/api-list", management.toString());
            assertEquals(app.baseUrl() + api + "/hello", invocation.toString());
            var listed = client.get(management.toString().substring(app.baseUrl().length()));
            assertEquals(200, listed.status, listed.text());
            assertEquals(true, listed.json().get("success"));
            var called = client.get(invocation.toString().substring(app.baseUrl().length()));
            assertEquals(200, called.status, called.text());
            assertEquals("\"configured\"", called.text());
            assertEquals(404, client.get(ui + "/config.json").status);
        }
    }

    private Map<?, ?> initializerOptions(String script) {
        int start = script.indexOf("window.DatawayUI(") + "window.DatawayUI(".length();
        return JsonUtils.readValue(script.substring(start, script.indexOf(");", start)), Map.class);
    }

    @Test
    void adminWithoutBusinessEntryDoesNotAdvertiseAnInvocationAddress() throws Throwable {
        Properties settings = TestSettings.enabled();
        settings.setProperty("dataway.api-enabled", "false");
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("reader").status);
            var initializer = client.get("/admin/initializer.js");
            assertEquals(200, initializer.status, initializer.text());
            Map<?, ?> options = this.initializerOptions(initializer.text());
            assertEquals("api/", options.get("adminApi"));
            assertFalse(options.containsKey("api"));
            assertEquals(200, client.get("/admin/api/api-list").status);
            var head = client.send("HEAD", "/admin/initializer.js", null);
            assertEquals(200, head.status);
            assertEquals(0, head.bytes.length);
            assertEquals(initializer.headers.get("Content-Type"), head.headers.get("Content-Type"));
            assertEquals(405, client.json("/admin/initializer.js", Map.of()).status);
        }
    }

}
