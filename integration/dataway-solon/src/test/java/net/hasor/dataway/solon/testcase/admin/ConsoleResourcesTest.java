/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.admin;
import java.util.Map;
import java.util.regex.Pattern;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.TestApplication;
import net.hasor.dataway.solon.testcase.TestSettings;
import org.junit.jupiter.api.Test;
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
}
