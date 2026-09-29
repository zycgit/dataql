/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.configuration;
import java.util.Properties;
import javax.sql.DataSource;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.solon.DatawayPlugin;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.TestApplication;
import net.hasor.dataway.solon.testcase.TestSettings;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.Plugin;
import static org.junit.jupiter.api.Assertions.*;

class StorageConfigurationTest {
    @Test
    void singleStorageBeanIsResolvedByTypeRegardlessOfItsName() throws Throwable {
        Properties settings = TestSettings.enabled();
        try (H2Database chosen = new H2Database(); TestApplication app = new TestApplication(context -> {
            context.wrapAndPut("chosen", chosen.access);
            new DatawayPlugin(TestSettings.configuration()).start(context);
        }, null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
            chosen.publish(app.dataway(), "GET", "/selected", "return 'selected';");
            assertEquals(200, client.login("api").status);
            var response = client.get("/api/selected");
            assertEquals(200, response.status, response.text());
            assertEquals("\"selected\"", response.text());
            assertEquals(1, chosen.count("interface_info"));
        }
    }

    @Test
    void explicitStorageAndCoreBypassContainerMetadataLookup() throws Throwable {
        Properties settings = TestSettings.enabled();
        try (H2Database database = new H2Database(); H2Database other = new H2Database()) {
            DatawayConfig config = TestSettings.configuration().dataAccessLayer(database.access);
            try (TestApplication app = new TestApplication(config, other.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                database.publish(app.dataway(), "GET", "/explicit", "return 'explicit';");
                assertEquals(200, client.login("api").status);
                assertEquals(200, client.get("/api/explicit").status);
                assertEquals(1, database.count("interface_info"));
                assertEquals(0, other.count("interface_info"));
            }
            Dataway core = config.createDataway();
            try (TestApplication app = new TestApplication(new DatawayPlugin(core), null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                assertSame(core, app.dataway());
                assertEquals(200, client.login("admin").status);
                assertEquals(200, client.get("/api/explicit").status);
                assertEquals(200, client.get("/docs/openapi.json").status);
                assertEquals(200, client.get("/admin/api/api-list").status);
            }
        }
    }

    @Test
    void dependencyInjectionAndInitializationCompleteBeforeTheFirstHttpCall() throws Throwable {
        try (H2Database database = new H2Database()) {
            Plugin registrations = context -> {
                new DatawayPlugin(TestSettings.configuration()).start(context);
                context.wrapAndPut(DataSource.class, database.source);
                context.beanMake(InitializedStorage.class);
                context.beanMake(StartupApi.class);
            };
            var settings = TestSettings.enabled();
            try (TestApplication app = new TestApplication(registrations, null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                assertEquals(200, client.login("api").status);
                var response = client.get("/api/ready");
                assertEquals(200, response.status, response.text());
                assertEquals("\"ready\"", response.text());
            }
        }
    }

    @Test
    void ambiguousStorageFailsAtBoot() throws Exception {
        try (H2Database first = new H2Database(); H2Database second = new H2Database()) {
            Plugin registrations = context -> {
                context.wrapAndPut("first", first.access);
                context.wrapAndPut("second", second.access);
                new DatawayPlugin(TestSettings.configuration()).start(context);
            };
            assertThrows(Throwable.class, () -> {
                try (TestApplication app = new TestApplication(registrations, null, TestSettings.enabled())) {
                    // A successful startup must make this assertion fail.
                }
            });
        }
    }

    @Test
    void enabledEntriesWithoutMetadataFailAtBoot() {
        assertThrows(Throwable.class, () -> {
            try (TestApplication app = new TestApplication((DatawayConfig) null, null, TestSettings.enabled())) {
                // A successful startup must make this assertion fail.
            }
        });
    }
}
