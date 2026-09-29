/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase.configuration;
import java.util.Properties;
import javax.sql.DataSource;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.spring.testcase.H2Database;
import net.hasor.dataway.spring.testcase.HttpClient;
import net.hasor.dataway.spring.testcase.TestApplication;
import net.hasor.dataway.spring.testcase.TestSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StorageConfigurationTest {
    @Test
    void singleStorageBeanIsResolvedByTypeRegardlessOfItsName() throws Throwable {
        Properties settings = TestSettings.enabled();
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), null, settings, context -> context.registerBean("chosen", ApiDataAccessLayer.class, () -> database.access)); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/selected", "return 'selected';");
            assertEquals(200, client.login("api").status);
            var response = client.get("/api/selected");
            assertEquals(200, response.status, response.text());
            assertEquals("\"selected\"", response.text());
            assertEquals(1, database.count("interface_info"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void missingOrAmbiguousStorageFailsAtBoot(boolean ambiguous) throws Exception {
        Properties settings = TestSettings.enabled();
        try (H2Database first = new H2Database(); H2Database second = new H2Database()) {
            assertThrows(RuntimeException.class, () -> {
                try (TestApplication app = new TestApplication(TestSettings.configuration(), ambiguous ? first.access : null, settings, context -> {
                    context.registerBean("notMetadata", String.class, () -> "not storage");
                    if (ambiguous) {
                        context.registerBean("other", ApiDataAccessLayer.class, () -> second.access);
                    }
                })) {
                    // A successful startup must make this assertion fail.
                }
            });
        }
    }

    @Test
    void explicitStorageAndCoreBypassMetadataLookup() throws Exception {
        Properties settings = TestSettings.enabled();
        try (H2Database database = new H2Database(); H2Database other = new H2Database()) {
            var config = TestSettings.configuration().dataAccessLayer(database.access);
            try (TestApplication app = new TestApplication(config, other.access, settings); HttpClient client = new HttpClient(app.baseUrl())) {
                database.publish(app.dataway(), "GET", "/explicit", "return 'explicit';");
                assertEquals(200, client.login("api").status);
                assertEquals(200, client.get("/api/explicit").status);
                assertEquals(1, database.count("interface_info"));
                assertEquals(0, other.count("interface_info"));
            }
            Dataway core = config.createDataway();
            try (TestApplication app = new TestApplication(config, null, settings, context -> context.registerBean(Dataway.class, () -> core)); HttpClient client = new HttpClient(app.baseUrl())) {
                assertSame(core, app.dataway());
                assertEquals(200, client.login("api").status);
                assertEquals(200, client.get("/api/explicit").status);
            }
        }
    }

    @Test
    void storageInitializationCompletesBeforeDatawayAndHttpRegistration() throws Exception {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), null, TestSettings.enabled(), context -> {
            context.registerBean(DataSource.class, () -> database.source);
            context.registerBean(InitializedStorage.class);
        }); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/ready", "return 'ready';");
            assertEquals(200, client.login("api").status);
            var response = client.get("/api/ready");
            assertEquals(200, response.status, response.text());
            assertEquals("\"ready\"", response.text());
        }
    }

    @Test
    void hostPrimaryMvcMappingIsUsedWithoutRequiringTheDefaultBeanName() throws Exception {
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(TestSettings.configuration(), database.access, TestSettings.enabled(), context -> context.registerBean(CustomMappingConfiguration.class)); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "GET", "/custom-mvc", "return 'host';");
            assertEquals(200, client.login("api").status);
            var response = client.get("/api/custom-mvc");
            assertEquals(200, response.status, response.text());
            assertEquals("\"host\"", response.text());
        }
    }
}
