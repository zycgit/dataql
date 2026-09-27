/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ResultInfoUtils;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.RequestAttribute;
import net.hasor.dataway.service.admin.AdminInterceptor;
import net.hasor.dataway.service.script.DatawayConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.Action;
import tools.jackson.databind.json.JsonMapper;
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
        var authorized = new ConcurrentHashMap<Operation, String>();
        AdminInterceptor policy = (action, chain) -> {
            if (!action.getIdentity().isAuthenticated()) {
                throw new DatawayException(401, "Host login required");
            }
            observed.put(action.getOperation(), action.getIdentity().getId());
            return chain.proceed();
        };

        IOException executionFailure = new IOException("script failed");
        Dataway dataway = Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()).configure(builder -> builder.configureRuntime(runtime -> {
            runtime.addShareVar("frameworkName", () -> (Udf) (hints, args) -> "solon");
        })).configureService(serviceBuilder -> serviceBuilder.interceptor((invocation, next) -> {
            if (invocation.getParameters().containsKey("fail")) {
                throw executionFailure;
            }
            if (invocation.getParameters().containsKey("download")) {
                return ResultInfoUtils.ofStream("application/pdf", new ByteArrayInputStream(new byte[] { 0, 1, (byte) 255 }));
            }
            return next.proceed(invocation);
        })).authorizationCheck((identity, operation) -> {
            if (!identity.isAuthenticated() || "denied-user".equals(identity.getId())) {
                return false;
            }
            authorized.put(operation, identity.getId());
            return true;
        }).actionInterceptor(policy).identityProvider(request -> {
            if (request.getPath().equals("/dataway" + request.getPathInfo())) {
                uiRequests.incrementAndGet();
            }
            return request.getIdentity();
        }).build();

        var service = dataway.getService();
        ApiDefinition helloApi = new ApiDefinition();
        helloApi.setId("hello");
        helloApi.setMethod("GET");
        helloApi.setPath("/hello");
        helloApi.setType(ApiScriptType.DATAQL);
        helloApi.setScript("return frameworkName();");
        helloApi.setDescription("");
        service.getBeanContainer().getAdminService().save(helloApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        service.getBeanContainer().getAdminService().publish("hello", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
        var app = Solon.start(DatawayPluginTest.class, new String[] { "--server.port=" + port, "--server.contextPath=/host", "--solon.app.name=dataway-adapter-test", "--dataway.api-enabled=true", "--dataway.admin-enabled=true" }, a -> {
            a.chains().addFilter((request, chain) -> {
                String user = request.header("X-Test-User");
                if (user != null) {
                    request.attrSet(RequestAttribute.IDENTITY.getKey(), UserIdentity.authenticated(user));
                }
                try {
                    chain.doFilter(request);
                } catch (DatawayException | IOException failure) {
                    request.status(failure instanceof DatawayException error ? error.status() : 500);
                    request.headerSet("X-Host-Error", "solon");
                    request.outputAsJson("{\"message\":\"Handled by the host\"}");
                }
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
            assertEquals("solon", new JsonMapper().readTree(get(client, base + "/api/hello", "host-user").body()).get("value").asText());
            var failed = get(client, base + "/api/hello?fail=true", "host-user");
            assertEquals(200, failed.statusCode());
            assertTrue(failed.headers().firstValue("X-Host-Error").isEmpty());
            var failureBody = new JsonMapper().readTree(failed.body());
            assertFalse(failureBody.get("success").asBoolean());
            assertEquals(500, failureBody.get("code").asInt());
            assertEquals("script failed", failureBody.get("message").asText());
            assertEquals("script failed", failureBody.get("value").asText());
            assertEquals("Unknown", failureBody.get("location").asText());
            assertEquals(-1, failureBody.get("executionTime").asLong());
            var binary = client.send(HttpRequest.newBuilder(URI.create(base + "/api/hello?download=true")).header("X-Test-User", "host-user").build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, binary.statusCode());
            assertEquals("application/pdf", binary.headers().firstValue("Content-Type").orElseThrow());
            ApiDefinition cookiesApi = new ApiDefinition();
            cookiesApi.setId("cookies");
            cookiesApi.setMethod("GET");
            cookiesApi.setPath("/cookies");
            cookiesApi.setType(ApiScriptType.DATAQL);
            cookiesApi.setScript("""
                    import 'net.hasor.dataway.function.WebUdfSource' as w;
                    var a = w.setHeader('X-Result', 'first');
                    var b = w.addHeader('X-Result', 'second');
                    var c = w.setCookie('one', '1');
                    var d = w.setCookie('two', '2', {'httpOnly': true});
                    return {'headers': w.headerArray('X-Repeat'), 'cookies': w.cookieArray('id')};
                    """);
            cookiesApi.setDescription("");
            service.getBeanContainer().getAdminService().save(cookiesApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            service.getBeanContainer().getAdminService().publish("cookies", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
            var cookies = client.send(HttpRequest.newBuilder(URI.create(base + "/api/cookies"))//
                    .header("X-Test-User", "host-user").header("X-Repeat", "one").header("X-Repeat", "two")//
                    .header("Cookie", "id=first; id=second").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, cookies.statusCode());
            assertEquals(List.of("first", "second"), cookies.headers().allValues("X-Result"));
            assertEquals(List.of("one=1; Path=/", "two=2; Path=/; HttpOnly"), cookies.headers().allValues("Set-Cookie"));
            var cookieBody = JsonMapper.builder().build().readTree(cookies.body()).get("value");
            assertEquals("one", cookieBody.get("headers").get(0).asText());
            assertEquals("two", cookieBody.get("headers").get(1).asText());
            assertEquals("first", cookieBody.get("cookies").get(0).asText());
            assertEquals("second", cookieBody.get("cookies").get(1).asText());

            assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, binary.body());
            assertEquals("host", get(client, base + "/health", null).body());
            assertEquals(200, get(client, base + "/dataway/", "host-user").statusCode());
            var redirect = get(client, base + "/dataway", "host-user");
            assertEquals(308, redirect.statusCode());
            assertEquals("dataway/", redirect.headers().firstValue("Location").orElseThrow());
            assertEquals(2, uiRequests.get());
            assertEquals("host-nested", get(client, base + "/dataway/host-route", null).body());
            assertEquals(401, get(client, base + "/dataway/api/api-list", null).statusCode());
            assertEquals(200, get(client, base + "/dataway/api/api-list", "host-user").statusCode());
            assertEquals(200, get(client, base + "/dataway/assets/app.js", null).statusCode());
            assertEquals(200, get(client, base + "/dataway/assets/app.css", null).statusCode());
            assertEquals(200, get(client, base + "/dataway/assets/app.js", "denied-user").statusCode());
            for (String path : List.of("/api/hello", "/dataway/api/api-list")) {
                var denied = get(client, base + path, "denied-user");
                assertEquals(401, denied.statusCode());
                assertEquals("solon", denied.headers().firstValue("X-Host-Error").orElseThrow());
            }
            assertEquals(Map.of(Operation.INVOKE, "host-user", Operation.LIST, "host-user"), observed);
            assertEquals(Map.of(Operation.INVOKE, "host-user", Operation.LIST, "host-user"), authorized);
            assertEquals(5, uiRequests.get(), "Native routing must keep host and management requests out of the UI handler");
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
            var service = DatawayService.builder(new DatawayConfig(), TestDatabase.dataAccessLayer()).build();
            ApiDefinition helloApi = new ApiDefinition();
            helloApi.setId("hello");
            helloApi.setMethod("GET");
            helloApi.setPath("/hello");
            helloApi.setType(ApiScriptType.DATAQL);
            helloApi.setScript("return 'solon';");
            helloApi.setDescription("");
            service.getBeanContainer().getAdminService().save(helloApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            service.getBeanContainer().getAdminService().publish("hello", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
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
                    a.router().get("/ops/manage/api-list", ctx -> ctx.output("host-admin"));
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
                assertEquals(admin, paths.contains("/tools/console/**"));
                assertFalse(paths.contains("/tools/console/assets/app.js"));
                assertEquals("host-boundary", get(client, base + "/open/v2-other", null).body());
                assertEquals("host-nested", get(client, base + "/tools/console/host-route", null).body());
                String apiBody = get(client, base + "/open/v2/hello", null).body();
                if (api) {
                    assertEquals("solon", new JsonMapper().readTree(apiBody).get("value").asText());
                } else {
                    assertEquals("host-api", apiBody);
                }
                assertEquals(200, get(client, base + "/ops/manage/api-list", null).statusCode());
                var management = get(client, base + "/ops/manage/api-list", null);
                assertEquals(200, management.statusCode());
                assertEquals(admin, management.body().contains("\"result\":"));
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
        var builder = Dataway.builder().dataAccessLayer(new JdbcDataAccessLayer(source, ""));
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
            ApiDefinition helloApi = new ApiDefinition();
            helloApi.setId("hello");
            helloApi.setMethod("GET");
            helloApi.setPath("/hello");
            helloApi.setType(ApiScriptType.DATAQL);
            helloApi.setScript("return 'from-config';");
            helloApi.setDescription("");
            service.getBeanContainer().getAdminService().save(helloApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            service.getBeanContainer().getAdminService().publish("hello", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            assertEquals("from-config", new JsonMapper().readTree(get(client, base + "/open/v2/hello", null).body()).get("value").asText());
            assertEquals(200, get(client, base + "/ops/manage/api-list", null).statusCode());
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
                var args = new ArrayList<String>();
                args.add("--server.port=" + port);
                args.add("--server.contextPath=/host");
                if (fromFile) {
                    args.add("--cfg=defaults/app.properties");
                }
                var builder = Dataway.builder().configureHost(host -> {
                    throw new AssertionError("Disabled entries must not initialize a runtime");
                });
                try (HttpClient client = HttpClient.newHttpClient()) {
                    var app = Solon.start(DatawayPluginTest.class, args.toArray(String[]::new), a -> {
                        a.pluginAdd(0, new DatawayPlugin(builder));
                        a.router().get("/api/hello", request -> request.output("host-api"));
                        a.router().get("/dataway/api/api-list", request -> request.output("host-admin"));
                        a.router().get("/dataway/", request -> request.output("host-ui"));
                    });
                    Dataway dataway = app.context().getBean(Dataway.class);
                    assertNull(dataway);
                    String base = "http://127.0.0.1:" + port + "/host";
                    assertEquals("host-api", get(client, base + "/api/hello", null).body());
                    assertEquals("host-admin", get(client, base + "/dataway/api/api-list", null).body());
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
                    var app = Solon.start(DatawayPluginTest.class, new String[] { "--cfg=configured/app.properties", "--server.port=" + port, "--server.contextPath=/host", "--dataway.api-enabled=" + enabled, "--dataway.admin-enabled=" + enabled }, a -> a.pluginAdd(0, new DatawayPlugin(Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()))));
                    Dataway dataway = app.context().getBean(Dataway.class);

                    String base = "http://127.0.0.1:" + port + "/host";
                    if (enabled) {
                        ApiDefinition configuredApi = new ApiDefinition();
                        configuredApi.setId("configured");
                        configuredApi.setMethod("GET");
                        configuredApi.setPath("/configured");
                        configuredApi.setType(ApiScriptType.DATAQL);
                        configuredApi.setScript("return 'configured';");
                        configuredApi.setDescription("");
                        dataway.getAdminService().save(configuredApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
                        dataway.getAdminService().publish("configured", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
                        assertEquals("configured", new JsonMapper().readTree(get(client, base + "/open/v2/configured", null).body()).get("value").asText());
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
                var builder = Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer());
                try (HttpClient client = HttpClient.newHttpClient()) {
                    Solon.start(DatawayPluginTest.class, new String[] { "--server.port=" + port, "--server.contextPath=" + contextPath, "--dataway.api-enabled=true", "--dataway.admin-enabled=true", "--dataway.api-prefix=/v1.0", "--dataway.admin-prefix=/ops/manage.v2", "--dataway.admin-ui=/console.v2" }, app -> app.pluginAdd(0, new DatawayPlugin(builder)));
                    String origin = "http://127.0.0.1:" + port;
                    var page = get(client, origin + contextPath + "/console.v2/", null);
                    assertEquals(200, page.statusCode());
                    assertFalse(page.body().contains("dataway-admin-api"));
                    assertFalse(page.body().contains("dataway-api"));
                    assertEquals(200, get(client, origin + contextPath + "/ops/manage.v2/api-list", null).statusCode());
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
        Set<String> expectedKeys = Set.of(//
                "dataway.api-enabled",    //
                "dataway.api-prefix",     //
                "dataway.admin-enabled",  //
                "dataway.admin-prefix",   //
                "dataway.admin-ui",       //
                "dataway.metadata.bean");
        assertEquals(expectedKeys, properties.stringPropertyNames());
        return properties;
    }

    private static Map<String, String> configurationSnapshot() throws IOException {
        Map<String, String> previous = new HashMap<>();
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
