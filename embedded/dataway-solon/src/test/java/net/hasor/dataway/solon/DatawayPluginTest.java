/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;

import java.net.*;
import java.net.http.*;
import java.time.Duration;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.execution.CallContext;
import net.hasor.dataway.function.FxRuntime;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ScriptType;
import net.hasor.dataway.repository.MemoryApiRepository;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.web.*;
import net.hasor.dataway.web.WebEntry;
import net.hasor.dataway.web.admin.AdminAuthorizer;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;

import static org.junit.jupiter.api.Assertions.*;

class DatawayPluginTest {
    @Test void actualSolonHttpServerServesApiConsoleAndHostRoutes() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        Dataway dataway = Dataway.builder().inMemory(true)
                .configure(builder -> builder.configureRuntime(runtime -> {
                    runtime.function("frameworkName", (hints, args) -> "solon");
                }))
                .webOptions(WebOptions.withAdminToken("solon-test-token")).build();
        var service = dataway.getService();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return frameworkName();", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        var app = Solon.start(DatawayPluginTest.class,
                new String[]{"--server.port=" + port, "--server.contextPath=/host", "--solon.app.name=dataway-adapter-test"},
                a -> { a.pluginAdd(0, new DatawayPlugin(dataway));
                    a.router().get("/health", ctx -> ctx.output("host")); });
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            assertSame(service, app.context().getBean(DatawayService.class));
            assertSame(dataway, app.context().getBean(Dataway.class));
            String base = "http://127.0.0.1:" + port + "/host";
            assertEquals("\"solon\"", get(client, base + "/api/hello", null).body());
            assertEquals("host", get(client, base + "/health", null).body());
            assertEquals(200, get(client, base + "/dataway/", null).statusCode());
            assertEquals(401, get(client, base + "/dataway/api/apis", null).statusCode());
            assertEquals(200, get(client, base + "/dataway/api/apis", "solon-test-token").statusCode());
        } finally { Solon.stopBlock(false, 0); }
    }
    @Test
    void eachEntryCanBeInstalledAloneAtAnIndependentPath() throws Exception {
        for (int mask : new int[] { 0, 1, 2, 4, 7 }) {
            int port;
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            boolean ui = (mask & 4) != 0;
            var service = DatawayService.builder(FxRuntime.builder().build(), new MemoryApiRepository()).build();
            service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'solon';", ""), 0, CallContext.LOCAL);
            service.publish("hello", 1, CallContext.LOCAL);
            var options = WebOptions.builder().apiEnabled(api).adminEnabled(admin).uiEnabled(ui)
                    .apiPrefix("/open/v2").adminPrefix("/ops/manage").uiPrefix("/tools/console")
                    .adminAuthorizer(AdminAuthorizer.bearerToken("solon-test-token")).build();
            var app = Solon.start(DatawayPluginTest.class,
                    new String[] { "--server.port=" + port, "--server.contextPath=/host", "--solon.app.name=dataway-entry-test" }, a -> {
                        a.pluginAdd(0, api || admin ? new DatawayPlugin(service, options) : new DatawayPlugin(options));
                        a.router().get("/open/v2/hello", ctx -> ctx.output("host-api"));
                        a.router().get("/ops/manage/apis", ctx -> ctx.output("host-admin"));
                        a.router().get("/tools/console/", ctx -> ctx.output("host-ui"));
                    });
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                String base = "http://127.0.0.1:" + port + "/host";
                assertEquals(api, app.context().getBean(Dataway.class).getHandlers().get(WebEntry.API) != null);
                assertEquals(admin, app.context().getBean(Dataway.class).getHandlers().get(WebEntry.ADMIN) != null);
                assertEquals(ui, app.context().getBean(Dataway.class).getHandlers().get(WebEntry.UI) != null);
                assertEquals(api || admin, app.context().getBean(DatawayService.class) != null);
                assertEquals(api ? "\"solon\"" : "host-api", get(client, base + "/open/v2/hello", null).body());
                assertEquals(admin ? 401 : 200, get(client, base + "/ops/manage/apis", null).statusCode());
                var management = get(client, base + "/ops/manage/apis", "solon-test-token");
                assertEquals(200, management.statusCode());
                assertEquals(admin, management.body().startsWith("["));
                var page = get(client, base + "/tools/console/", null);
                assertEquals(200, page.statusCode());
                if (ui) {
                    assertTrue(page.body().contains("content=\"../../ops/manage/\""));
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
        var configured = new java.util.Properties();
        try (var stream = getClass().getResourceAsStream("/dataway-prefixes.properties")) {
            configured.load(stream);
        }
        var previous = new java.util.HashMap<String, String>();
        configured.stringPropertyNames().forEach(key -> previous.put(key, System.getProperty(key)));
        try {
            for (boolean uiOnly : new boolean[] { false, true }) {
                int port;
                try (ServerSocket socket = new ServerSocket(0)) {
                    port = socket.getLocalPort();
                }
                var service = DatawayService.builder(FxRuntime.builder().build(), new MemoryApiRepository()).build();
                service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'from-config';", ""), 0, CallContext.LOCAL);
                service.publish("hello", 1, CallContext.LOCAL);
                var defaults = WebOptions.builder().apiPrefix("/code-api").adminPrefix("/code-admin").uiPrefix("/code-ui").build();
                var app = Solon.start(DatawayPluginTest.class, new String[] {
                        "--cfg=dataway-prefixes.properties", "--server.port=" + port, "--server.contextPath=/host",
                        "--dataway.embedded.api-enabled=" + !uiOnly, "--dataway.embedded.admin-enabled=" + !uiOnly
                }, a -> a.pluginAdd(0, uiOnly ? new DatawayPlugin() : new DatawayPlugin(service, defaults)));
                try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                    String base = "http://127.0.0.1:" + port + "/host";
                    assertEquals("/tools/console", app.context().getBean(Dataway.class).getHandlers().get(WebEntry.UI).pathPrefix());
                    var page = get(client, base + "/tools/console/", null);
                    assertEquals(200, page.statusCode());
                    assertTrue(page.body().contains("content=\"../../ops/manage/\""));
                    assertTrue(page.body().contains("content=\"../../open/v2/\""));
                    if (uiOnly) {
                        assertNull(app.context().getBean(DatawayService.class));
                        assertEquals(404, get(client, base + "/open/v2/hello", null).statusCode());
                        assertEquals(404, get(client, base + "/ops/manage/apis", null).statusCode());
                    } else {
                        assertEquals("\"from-config\"", get(client, base + "/open/v2/hello", null).body());
                        assertEquals(401, get(client, base + "/ops/manage/apis", null).statusCode());
                        assertEquals(200, get(client, base + "/ops/manage/apis", "solon-test-token").statusCode());
                        assertEquals(404, get(client, base + "/code-api/hello", null).statusCode());
                    }
                } finally {
                    Solon.stopBlock(false, 0);
                }
            }
        } finally {
            // Solon publishes loaded settings to JVM properties; restore only this fixture's keys.
            previous.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
