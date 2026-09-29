/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.configuration;
import java.util.Map;
import java.util.Properties;
import net.hasor.dataway.spring.testcase.H2Database;
import net.hasor.dataway.spring.testcase.HttpClient;
import net.hasor.dataway.spring.testcase.TestApplication;
import net.hasor.dataway.spring.testcase.TestSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomepageTest {
    @Test
    void homepageAssetsAndEntryConfigurationAreAccessibleBeforeLogin() throws Throwable {
        try (TestApplication app = new TestApplication(null, null, new Properties()); HttpClient client = new HttpClient(app.baseUrl())) {
            var page = client.get("/");
            assertEquals(200, page.status, page.text());
            assertTrue(page.headers.get("Content-Type").startsWith("text/html"));
            assertTrue(page.text().contains("id=\"login-form\""));
            assertTrue(page.text().contains("id=\"api-form\""));
            assertEquals("no-store", page.headers.get("Cache-Control"));
            assertEquals(200, client.get("/app.css").status);
            assertEquals(200, client.get("/app.js").status);
            var configuration = client.json("/example/config", Map.of());
            assertEquals(200, configuration.status, configuration.text());
            assertEquals("/host/api", configuration.json().get("apiPrefix"));
            assertEquals("/host/admin/", configuration.json().get("admin"));
            assertEquals(false, configuration.json().get("apiEnabled"));
            assertEquals(false, configuration.json().get("adminEnabled"));
            assertEquals(false, configuration.json().get("docsEnabled"));
            assertEquals(401, client.json("/session/me", Map.of()).status);
        }
    }

    @Test
    void homepageLinksFollowConfiguredPrefixesAndRetainHostAuthorization() throws Throwable {
        Properties settings = TestSettings.enabled();
        settings.setProperty("dataway.api-prefix", "/invoke");
        settings.setProperty("dataway.admin-prefix", "/console/manage");
        settings.setProperty("dataway.admin-ui", "/console");
        settings.setProperty("dataway.docs-prefix", "/specifications");
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            var configuration = client.json("/example/config", Map.of());
            assertEquals(200, configuration.status, configuration.text());
            assertEquals("/host/invoke", configuration.json().get("apiPrefix"));
            assertEquals("/host/console/", configuration.json().get("admin"));
            assertEquals("/host/specifications/openapi.json", configuration.json().get("openapi"));
            assertEquals("/host/specifications/swagger2.json", configuration.json().get("swagger"));
            assertEquals(true, configuration.json().get("apiEnabled"));
            assertEquals(true, configuration.json().get("adminEnabled"));
            assertEquals(true, configuration.json().get("docsEnabled"));
            assertEquals(401, client.get("/console/").status);
            assertEquals(200, client.login("api").status);
            var apiIdentity = client.json("/session/me", Map.of());
            assertEquals("api", apiIdentity.json().get("identity"));
            assertEquals(false, apiIdentity.json().get("consoleAccess"));
            assertEquals(true, apiIdentity.json().get("documentAccess"));
            assertEquals(401, client.get("/console/").status);
            assertEquals(200, client.get("/specifications/openapi.json").status);
            assertEquals(200, client.login("reader").status);
            assertEquals(true, client.json("/session/me", Map.of()).json().get("consoleAccess"));
            assertEquals(200, client.get("/console/").status);
        }
    }
}
