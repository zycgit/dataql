/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.configuration;
import java.util.Properties;
import net.hasor.dataway.hasor.testcase.H2Database;
import net.hasor.dataway.hasor.testcase.HttpClient;
import net.hasor.dataway.hasor.testcase.TestApplication;
import net.hasor.dataway.hasor.testcase.TestSettings;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class EntryConfigurationTest {
    @Test
    void defaultsStartARealServerWithoutRequiringMetadata() throws Throwable {
        try (TestApplication app = new TestApplication(null, null, new Properties()); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            for (String path : new String[] { "/api/hello", "/admin/api/api-list", "/admin/", "/docs/openapi.json" }) {
                var response = client.get(path);
                assertEquals(404, response.status, path + ": " + response.text());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7 })
    void entrySwitchesControlActualHttpReachability(int flags) throws Throwable {
        Properties settings = new Properties();
        settings.setProperty("dataway.api-enabled", Boolean.toString((flags & 1) != 0));
        settings.setProperty("dataway.admin-enabled", Boolean.toString((flags & 2) != 0));
        settings.setProperty("dataway.docs-enabled", Boolean.toString((flags & 4) != 0));
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            assertEquals(200, client.login("admin").status);
            if (flags != 0) {
                database.publish(app.dataway(), "GET", "/hello", "return 'hello';");
                assertSame(app.dataway(), app.dataway());
            }
            var api = client.get("/api/hello");
            assertEquals((flags & 1) == 0 ? 404 : 200, api.status, api.text());
            var admin = client.get("/admin/api/api-list");
            assertEquals((flags & 2) == 0 ? 404 : 200, admin.status, admin.text());
            assertEquals((flags & 2) == 0 ? 404 : 200, client.get("/admin/").status);
            var docs = client.get("/docs/openapi.json");
            assertEquals((flags & 4) == 0 ? 404 : 200, docs.status, docs.text());
        }
    }

    @Test
    void specificEntriesRemainReachableInsideTheUiPrefix() throws Throwable {
        Properties settings = TestSettings.enabled();
        settings.setProperty("dataway.api-prefix", "/console/invoke");
        settings.setProperty("dataway.admin-prefix", "/console/manage");
        settings.setProperty("dataway.admin-ui", "/console");
        settings.setProperty("dataway.docs-prefix", "/console/specifications");
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/hello", "return 'nested';");
            assertEquals(200, client.login("admin").status);
            var api = client.get("/console/invoke/hello");
            assertEquals(200, api.status, api.text());
            assertEquals("\"nested\"", api.text());
            var admin = client.get("/console/manage/api-list");
            assertEquals(200, admin.status, admin.text());
            assertEquals(true, admin.json().get("success"));
            var docs = client.get("/console/specifications/swagger2.json");
            assertEquals(200, docs.status, docs.text());
            assertEquals("2.0", docs.json().get("swagger"));
            var ui = client.get("/console/");
            assertEquals(200, ui.status, ui.text());
            assertTrue(ui.headers.get("Content-Type").startsWith("text/html"));
        }
    }

}
