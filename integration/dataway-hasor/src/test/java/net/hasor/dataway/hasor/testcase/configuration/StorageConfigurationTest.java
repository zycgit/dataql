/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.configuration;
import java.util.Properties;
import javax.sql.DataSource;
import net.hasor.core.Module;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.hasor.testcase.H2Database;
import net.hasor.dataway.hasor.testcase.HttpClient;
import net.hasor.dataway.hasor.testcase.TestApplication;
import net.hasor.dataway.hasor.testcase.TestSettings;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageConfigurationTest {
    @Test
    void containerConfigurationCanProvideStorageWithoutASeparateMetadataBean() throws Throwable {
        Properties settings = TestSettings.enabled();
        try (H2Database database = new H2Database()) {
            DatawayConfig config = TestSettings.configuration().dataAccessLayer(database.access);
            Module registrations = binder -> {
                binder.installModule(new DatawayModule());
                binder.bindType(DatawayConfig.class).toInstance(config);
            };
            try (TestApplication app = new TestApplication(registrations, null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                database.publish(app.dataway(), "GET", "/configured", "return 'configured';");
                assertEquals(200, client.login("api").status);
                var response = client.get("/api/configured");
                assertEquals(200, response.status, response.text());
                assertEquals("\"configured\"", response.text());
            }
        }
    }

    @Test
    void explicitConfigurationTakesPrecedenceOverTheContainerConfiguration() throws Throwable {
        try (H2Database database = new H2Database()) {
            DatawayConfig config = TestSettings.configuration().dataAccessLayer(database.access);
            Module registrations = binder -> {
                binder.bindType(DatawayConfig.class).toInstance(TestSettings.configuration().authorizationCheck((identity, operation) -> false));
                binder.installModule(new DatawayModule(config));
            };
            try (TestApplication app = new TestApplication(registrations, null, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
                database.publish(app.dataway(), "GET", "/explicit", "return 'explicit';");
                assertEquals(200, client.login("api").status);
                var response = client.get("/api/explicit");
                assertEquals(200, response.status, response.text());
                assertEquals("\"explicit\"", response.text());
            }
        }
    }

    @Test
    void absentConfigurationBeanUsesDefaultsWithContainerStorage() throws Throwable {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(new DatawayModule(), database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/defaults", "return 'defaults';");
            assertEquals(1, database.count("interface_info"));
            assertEquals(200, client.login("admin").status);
            assertEquals(200, client.get("/admin/").status);
            // Host request attributes require an explicitly configured identity provider.
            assertEquals(401, client.get("/api/defaults").status);
        }
    }

    @Test
    void singleStorageBeanIsResolvedByTypeRegardlessOfItsName() throws Throwable {
        Properties settings = TestSettings.enabled();
        try (H2Database chosen = new H2Database(); TestApplication app = new TestApplication(binder -> {
            new DatawayModule(TestSettings.configuration()).loadModule(binder);
            binder.bindType(ApiDataAccessLayer.class).nameWith("chosen").toInstance(chosen.access);
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
            try (TestApplication app = new TestApplication(new DatawayModule(core), null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
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
            Module registrations = binder -> {
                new DatawayModule(TestSettings.configuration()).loadModule(binder);
                binder.bindType(DataSource.class).toInstance(database.source);
                binder.bindType(ApiDataAccessLayer.class).to(InitializedStorage.class).asEagerSingleton();
            };
            var settings = TestSettings.enabled();

            try (TestApplication app = new TestApplication(registrations, null, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                database.publish(app.dataway(), "GET", "/ready", "return 'ready';");
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
            Module registrations = binder -> {
                binder.bindType(ApiDataAccessLayer.class).nameWith("first").toInstance(first.access);
                binder.bindType(ApiDataAccessLayer.class).nameWith("second").toInstance(second.access);
                binder.installModule(new DatawayModule(TestSettings.configuration()));
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
            try (TestApplication app = new TestApplication(null, null, TestSettings.enabled())) {
                // A successful startup must make this assertion fail.
            }
        });
    }
}
