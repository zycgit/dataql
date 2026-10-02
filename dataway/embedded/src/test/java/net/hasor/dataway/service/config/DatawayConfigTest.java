/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.function.WebFile;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatawayConfigTest extends ServiceTestSupport {
    @Test
    void configurationsOwnIndependentBuiltInHandlerRegistrations() {
        assertEquals(List.of("structure", "raw", "csv", "text", "verifyCode"), new ArrayList<>(this.config.getResultHandlers().keySet()));
        DatawayConfig other = new DatawayConfig();
        this.config.getResultHandlers().forEach((name, handler) -> {
            assertNotSame(handler, other.getResultHandlers().get(name));
            assertEquals(handler.getClass(), other.getResultHandlers().get(name).getClass());
        });
        StructureResultHandler replacement = new StructureResultHandler(Map.of("responseFormat", "{}"));
        this.config.resultHandler("structure", replacement);
        assertSame(replacement, this.config.getResultHandlers().get("structure"));
        assertNotSame(replacement, other.getResultHandlers().get("structure"));
    }

    @Test
    void defaultsCreateIndependentEntriesSharingTheConfiguredStorage() throws Exception {
        assertEquals("structure", this.config.getDefaultResultHandler());
        assertFalse(this.config.isWrapAllParameters());
        assertEquals("root", this.config.getWrapParameterName());
        assertSame(this.access, this.config.getDataAccessLayer());
        assertNull(this.config.getIdentityProvider());
        assertNull(this.config.getAuthorizationCheck());
        assertNull(this.config.getCustomizeScope());
        assertTrue(this.config.getAdminInterceptors().isEmpty());
        assertTrue(this.config.getApiInterceptors().isEmpty());
        assertTrue(this.config.getHostCustomizers().isEmpty());
        assertTrue(this.config.getQueryCustomizers().isEmpty());
        assertNull(this.config.getUploadTempDirectory());
        assertTrue(this.config.getUploadMemoryThreshold() > 0);

        Dataway dataway = this.config.createDataway();
        assertSame(dataway.getAdminService(), dataway.getAdminService());
        assertSame(dataway.getApiHandler(), dataway.getApiHandler());
        assertSame(dataway.getAdminHandler(), dataway.getAdminHandler());
        assertSame(dataway.getAdminUiHandler(), dataway.getAdminUiHandler());
        assertSame(dataway.getDocumentHandler(), dataway.getDocumentHandler());
        assertNotSame(dataway.getApiHandler(), dataway.getAdminHandler());
        assertEquals(List.of("", "/*"), dataway.getApiHandler().paths());
        assertTrue(dataway.getAdminService().list().isEmpty());
        assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getDocumentHandler(), "GET", "/openapi.json")).status());
        verify(this.access).configureMapping(Map.of(), Map.of());
    }

    @Test
    void storageIsMandatoryAndMappingFailuresStopInitialization() {
        assertThrows(IllegalStateException.class, () -> new DatawayConfig().createDataway());
        this.config.tableMapping(EntityType.INFO, "legacy_info");
        assertThrows(UnsupportedOperationException.class, this.config::createDataway);
        verify(this.access, never()).write(anyList());
    }

    @Test
    void mappingsAreAppliedToTheStorageBeforeTheServicesReadIt() {
        Map<EntityType, String> tables = Map.of(EntityType.INFO, "legacy_info", EntityType.RELEASE, "legacy_release");
        Map<EntityType, Map<FieldDef, String>> fields = Map.of(EntityType.INFO, Map.of(FieldDef.SCRIPT, "SCRIPT_ORI"));
        doNothing().when(this.access).configureMapping(tables, fields);
        this.config.tableMapping(EntityType.INFO, "legacy_info").tableMapping(EntityType.RELEASE, "legacy_release").fieldMapping(EntityType.INFO, FieldDef.SCRIPT, "SCRIPT_ORI").createDataway().getAdminService().list();
        var order = inOrder(this.access);
        order.verify(this.access).configureMapping(tables, fields);
        order.verify(this.access).listObjects(EntityType.INFO, Map.of());
    }

    @Test
    void configuredIdentityAndAuthorizationReachAllServiceEntries() throws Exception {
        UserIdentity identity = UserIdentity.authenticated("operator", Map.of());
        List<Operation> checked = new ArrayList<>();
        this.config.identityProvider(request -> identity).authorizationCheck((user, operation) -> {
            assertSame(identity, user);
            checked.add(operation);
            return false;
        });
        Dataway dataway = this.config.createDataway();
        assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getAdminHandler(), "GET", "/api-list")).status());
        assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getApiHandler(), "GET", "/test")).status());
        assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getDocumentHandler(), "GET", "/openapi.json")).status());
        assertEquals(List.of(Operation.LIST, Operation.INVOKE, Operation.DOCUMENT), checked);
    }

    @Test
    void runtimeConfigurationRegistersFunctionsImportsLibrariesAndCustomizers() throws Exception {
        this.config.identityProvider(request -> UserIdentity.authenticated("caller", Map.of()));
        Map<String, Udf> library = new HashMap<>();
        library.put("value", (hints, params) -> "library");
        AtomicInteger customized = new AtomicInteger();
        this.config.defaultResultHandler("raw").function("greet", (hints, params) -> "function").library("library", library).importSource("imported", () -> (Udf) (hints, params) -> "import").configureHost(host -> customized.incrementAndGet()).configureQuery(builder -> builder.addShareVar("extra", () -> "query"));
        library.put("value", (hints, params) -> "changed");

        Object result = this.invokeScript("""
                import 'library' as library;
                import 'imported' as imported;
                return [greet(), library.value(), imported(), extra];
                """).json();
        assertEquals(List.of("function", "library", "import", "query"), result);
        assertEquals(1, customized.get());
    }

    @Test
    void defaultsAndCustomScopeArePassedToTheExecutionEngine() throws Exception {
        this.config.identityProvider(request -> UserIdentity.authenticated("caller", Map.of()));
        this.config.wrapAllParameters(true).wrapParameterName("args").customizeScope(symbol -> Map.of("name", "configured"));
        this.config.resultHandler("structure", new StructureResultHandler(Map.of("responseFormat", """
                {"payload":"@resultData","ok":"@resultStatus"}
                """)));
        assertEquals(Map.of("payload", "configured", "ok", true), this.invokeScript("return ${args}.name;").json());
    }

    @Test
    void documentSettingsAppearInTheExport() throws Exception {
        this.config.identityProvider(request -> UserIdentity.consoleReadOnly("reader", Map.of()));
        this.config.documentTitle("Orders").documentVersion("2.0").documentServer("https://api.example.test/proxy");
        Map<?, ?> result = (Map<?, ?>) this.handle(this.config.createDataway().getDocumentHandler(), "GET", "/openapi.json").json();
        assertEquals(Map.of("title", "Orders", "version", "2.0"), result.get("info"));
        assertEquals(List.of(Map.of("url", "https://api.example.test/proxy")), result.get("servers"));
    }

    @Test
    void loaderSettingsFollowTheSelectedFinder() {
        ResourceLoader resources = mock(ResourceLoader.class);
        ClassLoader classes = mock(ClassLoader.class);
        this.config.resourceLoader(resources).classLoader(classes);
        assertSame(resources, this.config.getResourceLoader());
        assertSame(classes, this.config.getClassLoader());

        Finder custom = mock(Finder.class);
        when(custom.getResourceLoader()).thenReturn(ClassPathResourceLoader.INSTANCE);
        when(custom.getClassLoader()).thenReturn(this.getClass().getClassLoader());
        this.config.finder(custom);
        assertSame(custom, this.config.getFinder());
        assertSame(ClassPathResourceLoader.INSTANCE, this.config.getResourceLoader());
        assertSame(this.getClass().getClassLoader(), this.config.getClassLoader());
        assertThrows(IllegalStateException.class, () -> this.config.resourceLoader(resources));
        assertThrows(IllegalStateException.class, () -> this.config.classLoader(classes));

        this.config.finder(null).resourceLoader(null).classLoader(null);
        assertInstanceOf(DatawayFinder.class, this.config.getFinder());
        assertSame(ClassPathResourceLoader.INSTANCE, this.config.getResourceLoader());
        assertSame(DatawayFinder.class.getClassLoader(), this.config.getClassLoader());
    }

    @Test
    void defaultFinderUsesItsClassLoaderAndRejectsUnknownFragments() throws Exception {
        DatawayFinder finder = new DatawayFinder();
        ClassLoader classes = mock(ClassLoader.class);
        doReturn(StringBuilder.class).when(classes).loadClass("builder");
        finder.setClassLoader(classes);
        assertInstanceOf(StringBuilder.class, finder.findBean("builder"));
        assertInstanceOf(StringBuilder.class, finder.findBean(StringBuilder.class));
        verify(classes).loadClass("builder");
        assertThrows(UnsupportedOperationException.class, () -> finder.findFragmentProcess("SQL"));
    }

    @Test
    void uploadConfigurationIsRetainedAndInvalidThresholdsFailAtAssembly(@TempDir Path directory) {
        this.config.uploadTempDirectory(directory).uploadMemoryThreshold(0);
        assertEquals(directory, this.config.getUploadTempDirectory());
        assertEquals(0, this.config.getUploadMemoryThreshold());
        assertNotNull(this.config.createDataway());
        this.config.uploadMemoryThreshold(-1);
        assertThrows(IllegalArgumentException.class, this.config::createDataway);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void configuredUploadSpillsToDiskAndIsReleasedAfterSuccessOrFailure(boolean authorized, @TempDir Path directory) throws Exception {
        AtomicReference<WebFile> uploaded = new AtomicReference<>();
        this.config.uploadTempDirectory(directory).uploadMemoryThreshold(1).authorizationCheck((identity, operation) -> authorized).identityProvider(request -> assertDoesNotThrow(() -> {
            WebFile file = ((MemoryRequest) request).upload("larger than the configured threshold");
            uploaded.set(file);
            try (var files = Files.list(directory)) {
                assertEquals(1, files.count());
            }
            return UserIdentity.anonymous(Map.of());
        }));
        Dataway dataway = this.config.createDataway();
        if (authorized) {
            assertEquals(200, this.handle(dataway.getAdminHandler(), "GET", "/api-list").getStatus());
        } else {
            assertEquals(401, assertThrows(DatawayException.class, () -> this.handle(dataway.getAdminHandler(), "GET", "/api-list")).status());
        }
        try (var files = Files.list(directory)) {
            assertEquals(0, files.count());
        }
        assertThrows(IOException.class, () -> uploaded.get().openStream());
    }

    @Test
    void changingConfigurationDoesNotReconfigureAnExistingDataway() throws Exception {
        this.config.identityProvider(request -> UserIdentity.authenticated("caller", Map.of()));
        Map<FieldDef, String> info = this.info("api", "1", 1);
        info.put(FieldDef.SCRIPT, "return version();");
        this.publishRoute(this.release(info, "release", "1", 1));
        this.config.function("version", (hints, params) -> "first");
        Dataway first = this.config.createDataway();
        this.config.function("version", (hints, params) -> "second").defaultResultHandler("raw");
        Dataway second = this.config.createDataway();
        Map<?, ?> original = (Map<?, ?>) this.handle(first.getApiHandler(), "GET", "/api").json();
        assertEquals("first", original.get("value"));
        assertEquals("second", this.handle(second.getApiHandler(), "GET", "/api").json());
    }

    @Test
    void beanReplacementClearsMultipleRegistrationsAndSnapshotsKeepTheirOrder() {
        BeanContainer beans = new BeanContainer();
        assertTrue(beans.getBeans(String.class).isEmpty());
        assertThrows(IllegalStateException.class, () -> beans.getBean(String.class));
        beans.addBean(String.class, "one");
        beans.addBean(String.class, "two");
        List<String> original = beans.getBeans(String.class);
        assertEquals(List.of("one", "two"), original);
        assertThrows(IllegalStateException.class, () -> beans.getBean(String.class));
        beans.setBean(String.class, "only");
        assertEquals("only", beans.getBean(String.class));
        assertEquals(List.of("only"), beans.getBeans(String.class));
        assertEquals(List.of("one", "two"), original);
    }
}
