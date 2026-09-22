/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.FxRuntime;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayException;
import net.hasor.dataway.spi.DatawayInterceptor;
import net.hasor.dataway.web.DatawayUiHandler;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.dataway.web.SerializationInfo;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;
import static org.junit.jupiter.api.Assertions.*;

class DatawayPluginTest {
    @Test
    void actualSolonHttpServerServesApiConsoleAndHostRoutes() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        AtomicInteger uiRequests = new AtomicInteger();
        var observed = new ConcurrentHashMap<Operation, String>();
        DatawayInterceptor policy = (action, chain) -> {
            if (action.getCallContext().source().equals("HTTP")) {
                if (!action.getIdentity().isAuthenticated()) {
                    throw new DatawayException(401, "Host login required");
                }
                observed.put(action.getOperation(), action.getIdentity().getId());
            }
            return chain.proceed();
        };

        Dataway dataway = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).configure(builder -> builder.configureRuntime(runtime -> {
            runtime.function("frameworkName", (hints, args) -> "solon");
        })).configureService(serviceBuilder -> serviceBuilder.interceptor((invocation, next) -> {
            if (invocation.parameters().containsKey("download")) {
                return SerializationInfo.ofStream("application/pdf", new java.io.ByteArrayInputStream(new byte[] { 0, 1, (byte) 255 }));
            }
            return next.proceed(invocation);
        })).accessInterceptor(policy).actionInterceptor(policy).uiHandler(context -> {
            var ui = new DatawayUiHandler(context);
            return (request, response) -> {
                uiRequests.incrementAndGet();
                ui.handle(request, response);
            };
        }).build();

        var service = dataway.getService();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return frameworkName();", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        var app = Solon.start(DatawayPluginTest.class, new String[] { "--server.port=" + port, "--server.contextPath=/host", "--solon.app.name=dataway-adapter-test", "--dataway.api-enabled=true", "--dataway.admin-enabled=true" }, a -> {
            a.chains().addFilter((request, chain) -> {
                String user = request.header("X-Test-User");
                if (user != null) {
                    request.attrSet(RequestAttribute.IDENTITY.getKey(), UserIdentity.authenticated(user));
                }
                chain.doFilter(request);
            }, -100);
            a.router().routerInterceptor((request, handler, chain) -> {
                if (request.pathNew().startsWith("/api/") || request.pathNew().startsWith("/dataway")) {
                    if (!request.pathNew().endsWith("host-route") && handler != null) {
                        assertInstanceOf(Action.class, handler);
                    }
                }
                chain.doIntercept(request, handler);
            });
            a.pluginAdd(0, new DatawayPlugin(dataway));
            a.router().get("/health", ctx -> ctx.output("host"));
            a.router().get("/dataway/host-route", ctx -> ctx.output("host-nested"));
        });

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            assertSame(service, app.context().getBean(Dataway.class).getService());
            assertNull(app.context().getBean(DatawayService.class));
            assertSame(dataway, app.context().getBean(Dataway.class));
            String base = "http://127.0.0.1:" + port + "/host";
            assertEquals("\"solon\"", get(client, base + "/api/hello", "host-user").body());
            var binary = client.send(HttpRequest.newBuilder(URI.create(base + "/api/hello?download=true")).header("X-Test-User", "host-user").build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, binary.statusCode());
            assertEquals("application/pdf", binary.headers().firstValue("Content-Type").orElseThrow());
            assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, binary.body());
            assertEquals("host", get(client, base + "/health", null).body());
            assertEquals(200, get(client, base + "/dataway/", "host-user").statusCode());
            var redirect = get(client, base + "/dataway", "host-user");
            assertEquals(308, redirect.statusCode());
            assertEquals("dataway/", redirect.headers().firstValue("Location").orElseThrow());
            assertEquals(2, uiRequests.get());
            assertEquals("host-nested", get(client, base + "/dataway/host-route", null).body());
            assertEquals(401, get(client, base + "/dataway/api/apis", null).statusCode());
            assertEquals(200, get(client, base + "/dataway/api/apis", "host-user").statusCode());
            assertEquals(401, get(client, base + "/dataway/assets/app.js", null).statusCode());
            assertEquals(401, get(client, base + "/dataway/assets/app.css", null).statusCode());
            assertEquals(Map.of(Operation.RESOURCE, "host-user", Operation.INVOKE, "host-user", Operation.LIST, "host-user"), observed);
            assertEquals(2, uiRequests.get(), "Native routing must keep host and management requests out of the UI handler");
        } finally {
            Solon.stopBlock(false, 0);
        }
    }

    @Test
    void managementAndResourcesShareTheirSwitchButUseSeparateRoutes() throws Exception {
        for (int mask = 0; mask < 4; mask++) {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            var service = DatawayService.builder(FxRuntime.builder().build(), TestDatabase.dataAccessLayer()).build();
            service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'solon';", ""), 0, CallContext.LOCAL);
            service.publish("hello", 1, CallContext.LOCAL);
            var app = Solon.start(DatawayPluginTest.class, new String[] { "--server.port=" + port, "--server.contextPath=/host", "--solon.app.name=dataway-entry-test" }, a -> {
                a.cfg().setProperty("dataway.api-enabled", Boolean.toString(api));
                a.cfg().setProperty("dataway.admin-enabled", Boolean.toString(admin));
                a.cfg().setProperty("dataway.api-prefix", "/open/v2");
                a.cfg().setProperty("dataway.admin-prefix", "/ops/manage");
                a.cfg().setProperty("dataway.admin-ui", "/tools/console");
                var builder = Dataway.builder();
                if (api || admin) {
                    builder.service(service);
                }
                a.pluginAdd(0, new DatawayPlugin(builder));
                if (!api) {
                    a.router().get("/open/v2/hello", ctx -> ctx.output("host-api"));
                }
                if (!admin) {
                    a.router().get("/ops/manage/apis", ctx -> ctx.output("host-admin"));
                }
                if (!admin) {
                    a.router().get("/tools/console/", ctx -> ctx.output("host-ui"));
                }
                a.router().get("/open/v2-other", ctx -> ctx.output("host-boundary"));
                a.router().get("/tools/console/host-route", ctx -> ctx.output("host-nested"));
            });
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                String base = "http://127.0.0.1:" + port + "/host";
                Dataway core = app.context().getBean(Dataway.class);
                if (api || admin) {
                    assertSame(service, core.getService());
                    assertNotNull(core.getApiHandler());
                    assertNotNull(core.getAdminHandler());
                    assertNotNull(core.getUiHandler());
                } else {
                    assertNull(core);
                }
                assertNull(app.context().getBean(DatawayService.class));
                var paths = app.router().findAll().stream().map(route -> route.path()).toList();
                assertFalse(paths.contains("/**"));
                assertEquals(api, paths.contains("/open/v2/**"));
                assertEquals(admin, paths.contains("/ops/manage/**"));
                assertEquals(admin, paths.contains("/tools/console/assets/app.js"));
                assertFalse(paths.contains("/tools/console/**"));
                assertEquals("host-boundary", get(client, base + "/open/v2-other", null).body());
                assertEquals("host-nested", get(client, base + "/tools/console/host-route", null).body());
                assertEquals(api ? "\"solon\"" : "host-api", get(client, base + "/open/v2/hello", null).body());
                assertEquals(200, get(client, base + "/ops/manage/apis", null).statusCode());
                var management = get(client, base + "/ops/manage/apis", null);
                assertEquals(200, management.statusCode());
                assertEquals(admin, management.body().startsWith("["));
                var page = get(client, base + "/tools/console/", null);
                assertEquals(200, page.statusCode());
                if (admin) {
                    assertFalse(page.body().contains("dataway-admin-api"));
                    assertEquals(200, get(client, base + "/tools/console/assets/app.js", null).statusCode());
                } else {
                    assertEquals("host-ui", page.body());
                }
            } finally {
                Solon.stopBlock(false, 0);
            }
        }
    }

    @Test
    void nativeConfigurationFileSetsPrefixesBeforeRegistration() throws Exception {
        configuration("configured/app.properties");
        Map<String, String> previous = configurationSnapshot();
        int port = availablePort();
        JdbcDataSource source = dataSource();
        var builder = Dataway.builder().dataSource(source).dataAccessLayer(new net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer(source, ""));
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var app = Solon.start(DatawayPluginTest.class, new String[] { "--cfg=configured/app.properties", "--server.port=" + port, "--server.contextPath=/host" }, a -> a.pluginAdd(0, new DatawayPlugin(builder)));
            Dataway dataway = app.context().getBean(Dataway.class);
            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getUiHandler());
            String base = "http://127.0.0.1:" + port + "/host";
            var page = get(client, base + "/tools/console/", null);
            assertEquals(200, page.statusCode());
            assertFalse(page.body().contains("dataway-admin-api"));
            assertFalse(page.body().contains("dataway-api"));
            var service = dataway.getService();
            service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'from-config';", ""), 0, CallContext.LOCAL);
            service.publish("hello", 1, CallContext.LOCAL);
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            assertEquals("\"from-config\"", get(client, base + "/open/v2/hello", null).body());
            assertEquals(200, get(client, base + "/ops/manage/apis", null).statusCode());
            assertEquals(404, get(client, base + "/code-api/hello", null).statusCode());
        } finally {
            Solon.stopBlock(false, 0);
            restoreConfiguration(previous);
        }
    }

    @Test
    void omittedSettingsAndCompleteDefaultFileLeaveEveryEntryInactive() throws Exception {
        configuration("defaults/app.properties");
        Map<String, String> previous = configurationSnapshot();
        try {
            for (boolean fromFile : new boolean[] { false, true }) {
                int port = availablePort();
                var args = new java.util.ArrayList<String>();
                args.add("--server.port=" + port);
                args.add("--server.contextPath=/host");
                if (fromFile) {
                    args.add("--cfg=defaults/app.properties");
                }
                var builder = Dataway.builder().dataSource(() -> {
                    throw new AssertionError("Disabled entries must not resolve a datasource");
                });
                try (HttpClient client = HttpClient.newHttpClient()) {
                    var app = Solon.start(DatawayPluginTest.class, args.toArray(String[]::new), a -> {
                        a.pluginAdd(0, new DatawayPlugin(builder));
                        a.router().get("/api/hello", request -> request.output("host-api"));
                        a.router().get("/dataway/api/apis", request -> request.output("host-admin"));
                        a.router().get("/dataway/", request -> request.output("host-ui"));
                    });
                    Dataway dataway = app.context().getBean(Dataway.class);
                    assertNull(dataway);
                    String base = "http://127.0.0.1:" + port + "/host";
                    assertEquals("host-api", get(client, base + "/api/hello", null).body());
                    assertEquals("host-admin", get(client, base + "/dataway/api/apis", null).body());
                    assertEquals("host-ui", get(client, base + "/dataway/", null).body());
                } finally {
                    Solon.stopBlock(false, 0);
                }
            }
        } finally {
            restoreConfiguration(previous);
        }
    }

    @Test
    void eachEntryCanBeExplicitlyDisabledDespiteTheEnabledConfigurationFile() throws Exception {
        configuration("configured/app.properties");
        Map<String, String> previous = configurationSnapshot();
        try {
            for (boolean enabled : new boolean[] { true, false }) {
                int port = availablePort();
                try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                    var app = Solon.start(DatawayPluginTest.class, new String[] { "--cfg=configured/app.properties", "--server.port=" + port, "--server.contextPath=/host", "--dataway.api-enabled=" + enabled, "--dataway.admin-enabled=" + enabled }, a -> a.pluginAdd(0, new DatawayPlugin(Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()))));
                    Dataway dataway = app.context().getBean(Dataway.class);

                    String base = "http://127.0.0.1:" + port + "/host";
                    if (enabled) {
                        dataway.getService().save(new ApiDefinition("configured", "GET", "/configured", ScriptType.DATAQL, "return 'configured';", ""), 0, CallContext.LOCAL);
                        dataway.getService().publish("configured", 1, CallContext.LOCAL);
                        assertEquals("\"configured\"", get(client, base + "/open/v2/configured", null).body());
                        assertNotNull(dataway.getApiHandler());
                        assertNotNull(dataway.getAdminHandler());
                        assertNotNull(dataway.getUiHandler());
                    } else {
                        assertNull(dataway);
                        assertEquals(404, get(client, base + "/open/v2/configured", null).statusCode());
                    }
                } finally {
                    Solon.stopBlock(false, 0);
                }
            }
        } finally {
            restoreConfiguration(previous);
        }
    }

    @Test
    void uiRemainsStaticAcrossServerContextsAndPrefixes() throws Exception {
        Map<String, String> previous = configurationSnapshot();
        try {
            for (String contextPath : new String[] { "", "/tenant/host" }) {
                int port = availablePort();
                var builder = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer());
                try (HttpClient client = HttpClient.newHttpClient()) {
                    Solon.start(DatawayPluginTest.class, new String[] { "--server.port=" + port, "--server.contextPath=" + contextPath, "--dataway.api-enabled=true", "--dataway.admin-enabled=true", "--dataway.api-prefix=/v1.0", "--dataway.admin-prefix=/ops/manage.v2", "--dataway.admin-ui=/console.v2" }, app -> app.pluginAdd(0, new DatawayPlugin(builder)));
                    String origin = "http://127.0.0.1:" + port;
                    var page = get(client, origin + contextPath + "/console.v2/", null);
                    assertEquals(200, page.statusCode());
                    assertFalse(page.body().contains("dataway-admin-api"));
                    assertFalse(page.body().contains("dataway-api"));
                    assertEquals(200, get(client, origin + contextPath + "/ops/manage.v2/apis", null).statusCode());
                } finally {
                    Solon.stopBlock(false, 0);
                }
            }
        } finally {
            restoreConfiguration(previous);
        }
    }

    private static Properties configuration(String resource) throws IOException {
        Properties properties = new Properties();
        try (var stream = DatawayPluginTest.class.getResourceAsStream("/" + resource)) {
            assertNotNull(stream, resource);
            properties.load(stream);
        }
        Set<String> expectedKeys = Set.of("dataway.api-enabled", "dataway.api-prefix", "dataway.admin-enabled", "dataway.admin-prefix", "dataway.admin-ui",
                "dataway.metadata.type",
                "dataway.metadata.bean",
                "dataway.metadata.jdbc.executor",
                "dataway.metadata.jdbc.data-source",
                "dataway.metadata.jdbc.transaction-manager",
                "dataway.metadata.jdbc.table-prefix",
                "dataway.metadata.nacos.config-service",
                "dataway.metadata.nacos.data-id",
                "dataway.metadata.nacos.group",
                "dataway.metadata.nacos.timeout-millis");
        assertEquals(expectedKeys, properties.stringPropertyNames());
        return properties;
    }

    private static Map<String, String> configurationSnapshot() throws IOException {
        Map<String, String> previous = new java.util.HashMap<>();
        for (String key : configuration("configured/app.properties").stringPropertyNames()) {
            previous.put(key, System.getProperty(key));
        }
        previous.put("cfg", System.getProperty("cfg"));
        return previous;
    }

    private static void restoreConfiguration(Map<String, String> previous) {
        // Solon publishes file configuration to JVM properties; keep the next test isolated.
        previous.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    private static JdbcDataSource dataSource() {
        return TestDatabase.create();
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String user) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (user != null) {
            request.header("X-Test-User", user);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
