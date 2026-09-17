/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.hasor.cobble.setting.DefaultSettings;
import net.hasor.core.ApiBinder;
import net.hasor.core.Hasor;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayConfigurer;
import net.hasor.dataway.execution.CallContext;
import net.hasor.dataway.function.FxRuntime;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ScriptType;
import net.hasor.dataway.repository.MemoryApiRepository;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.web.WebOptions;
import net.hasor.dataway.web.WebEntry;
import net.hasor.dataway.web.admin.AdminAuthorizer;
import net.hasor.web.Invoker;
import net.hasor.web.InvokerFilter;
import net.hasor.web.WebApiBinder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatawayModuleTest {
    @Test
    void commonCoreSetupCanBePassedAsBuilderOrAssembledInstance() throws Throwable {
        DatawayConfigurer setup = builder -> builder.inMemory(true)
                .configureRuntime(runtime -> runtime.function("frameworkName", (hints, args) -> "hasor-spi"));
        for (boolean assembled : new boolean[] { false, true }) {
            var builder = Dataway.builder().configure(setup);
            Dataway prepared = assembled ? builder.build() : null;
            var module = assembled ? new DatawayModule(prepared) : new DatawayModule(builder);
            try (var context = Hasor.create().build(module)) {
                Dataway dataway = context.getInstance(Dataway.class);
                if (assembled) {
                    assertSame(prepared, dataway);
                }
                var service = dataway.getService();
                service.save(new ApiDefinition("spi", "GET", "/spi", ScriptType.DATAQL,
                        "return frameworkName();", ""), 0, CallContext.LOCAL);
                service.publish("spi", 1, CallContext.LOCAL);
                assertEquals("hasor-spi", service.invokeApi("/spi", Map.of()));
            }
        }
    }

    @Test
    void actualHasorContainerInstallsIndependentService() throws Throwable {
        var runtime = FxRuntime.builder().build();
        var service = DatawayService.builder(runtime, new MemoryApiRepository()).build();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'hasor';", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        try (var context = Hasor.create().build(new DatawayModule(service))) {
            assertSame(service, context.getInstance(DatawayService.class));
            assertEquals("hasor", context.getInstance(DatawayService.class).invokeApi("/hello", Map.of()));
        }
        // Closing the adapter does not own or close the supplied runtime.
        assertEquals("hasor", service.invokeApi("/hello", Map.of()));
    }

    @Test
    void registeredWebFilterTranslatesContextPathAndPassesUnmatchedRequests() throws Throwable {
        var service = DatawayService.builder(FxRuntime.builder().build(), new MemoryApiRepository()).build();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return ${name};", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        var settings = new DefaultSettings();
        settings.setSetting("dataway.embedded.api-prefix", "/open/v2");
        AtomicReference<InvokerFilter> captured = new AtomicReference<>();
        // Capture the actual filter registered through the public Hasor binder contract.
        WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
            case "tryCast" -> p;
            case "getSettings" -> settings;
            case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> null);
            case "filter" -> proxy(WebApiBinder.FilterBindingBuilder.class, (x, m, a) -> {
                for (Object value : a) {
                    if (value instanceof InvokerFilter filter) {
                        captured.set(filter);
                    }
                }
                return null;
            });
            default -> null;
        });

        new DatawayModule(service).loadModule(binder);
        assertNotNull(captured.get());
        AtomicReference<String> path = new AtomicReference<>("/host/open/v2/hello");
        var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
            case "getRequestURI" -> path.get();
            case "getContextPath" -> "/host";
            case "getMethod" -> "GET";
            case "getQueryString" -> "name=Ada";
            case "getHeaderNames" -> Collections.emptyEnumeration();
            case "getInputStream" -> new ServletInputStream() {
                public int read() {
                    return -1;
                }

                public boolean isFinished() {
                    return true;
                }

                public boolean isReady() {
                    return true;
                }

                public void setReadListener(ReadListener listener) {
                }
            };
            default -> null;
        });

        var output = new ByteArrayOutputStream();
        var status = new AtomicInteger();
        var response = proxy(HttpServletResponse.class, (p, method, args) -> {
            if (method.getName().equals("setStatus")) {
                status.set((Integer) args[0]);
            }
            if (method.getName().equals("getOutputStream")) {
                return new ServletOutputStream() {
                    public void write(int b) {
                        output.write(b);
                    }

                    public boolean isReady() {
                        return true;
                    }

                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
            return null;
        });

        var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
            case "getHttpRequest" -> request;
            case "getHttpResponse" -> response;
            default -> null;
        });
        assertNull(captured.get().doInvoke(invoker, next -> "host"));
        assertEquals(200, status.get());
        assertEquals("\"Ada\"", output.toString(java.nio.charset.StandardCharsets.UTF_8));
        path.set("/host/other");
        assertEquals("host", captured.get().doInvoke(invoker, next -> "host"));
    }

    @Test
    void enabledCoreEntriesHaveSeparateMappings() throws Throwable {
        var service = DatawayService.builder(FxRuntime.builder().build(), new MemoryApiRepository()).build();
        for (int mask = 0; mask < 8; mask++) {
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            boolean ui = (mask & 4) != 0;
            var bindings = new java.util.HashMap<Class<?>, Object>();
            var filters = new java.util.LinkedHashMap<String, InvokerFilter>();
            WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
                case "tryCast" -> p;
                case "getSettings" -> new DefaultSettings();
                case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> {
                    if (m.getName().equals("toInstance")) {
                        bindings.put((Class<?>) args[0], a[0]);
                    }
                    return null;
                });
                case "filter" -> {
                    String[] paths = java.util.stream.Stream.concat(java.util.stream.Stream.of((String) args[0]), java.util.Arrays.stream((String[]) args[1])).toArray(String[]::new);
                    assertArrayEquals(new String[] { paths[0], paths[0] + "/*" }, paths);
                    yield proxy(WebApiBinder.FilterBindingBuilder.class, (x, m, a) -> {
                        for (Object value : a) {
                            if (value instanceof InvokerFilter filter) {
                                filters.put(paths[0], filter);
                            }
                        }
                        return null;
                    });
                }
                default -> null;
            });

            var options = WebOptions.builder().apiEnabled(api).adminEnabled(admin).uiEnabled(ui).apiPrefix("/open/v2").adminPrefix("/ops/manage").uiPrefix("/tools/console").adminAuthorizer(AdminAuthorizer.bearerToken("hasor-test-token")).build();
            new DatawayModule(service, options).loadModule(binder);
            assertSame(service, bindings.get(DatawayService.class));
            assertEquals(api, ((Dataway) bindings.get(Dataway.class)).getHandlers().containsKey(WebEntry.API));
            assertEquals(admin, ((Dataway) bindings.get(Dataway.class)).getHandlers().containsKey(WebEntry.ADMIN));
            assertEquals(ui, ((Dataway) bindings.get(Dataway.class)).getHandlers().containsKey(WebEntry.UI));
            assertEquals(api, filters.containsKey("/open/v2"));
            assertEquals(admin, filters.containsKey("/ops/manage"));
            assertEquals(ui, filters.containsKey("/tools/console"));
            assertEquals(Integer.bitCount(mask), filters.size());
        }
    }

    @Test
    void uiOnlyCanBeInstalledWithoutAService() throws Throwable {
        var settings = new java.util.Properties();
        settings.setProperty("dataway.embedded.api-enabled", "false");
        settings.setProperty("dataway.embedded.ui-enabled", "true");
        settings.setProperty("dataway.embedded.ui-prefix", "/console-only");
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule())) {
            assertEquals("/console-only", context.getInstance(Dataway.class).getHandlers().get(WebEntry.UI).pathPrefix());
        }
    }

    @Test
    void nativeConfigurationFileOverridesCodeDefaultsAndConfiguresAllEntries() throws Throwable {
        var service = DatawayService.builder(FxRuntime.builder().build(), new MemoryApiRepository()).build();
        var defaults = WebOptions.builder().apiPrefix("/code-api").adminPrefix("/code-admin").uiPrefix("/code-ui").build();
        try (var context = Hasor.create().mainSettingWith("dataway-prefixes.properties").build(new DatawayModule(service, defaults))) {
            var api = context.getInstance(Dataway.class).getHandlers().get(WebEntry.API);
            var admin = context.getInstance(Dataway.class).getHandlers().get(WebEntry.ADMIN);
            var ui = context.getInstance(Dataway.class).getHandlers().get(WebEntry.UI);
            assertEquals("/open/v2", api.pathPrefix());
            assertEquals("/ops/manage", admin.pathPrefix());
            assertEquals("/tools/console", ui.pathPrefix());
            assertFalse(api.matches("/code-api/test"));
            var request = new net.hasor.dataway.web.WebRequest("GET", "/ops/manage/apis", null, Map.of("Authorization", "Bearer hasor-test-token"), java.io.InputStream.nullInputStream(), null);
            assertEquals(200, admin.handle(request).status());
            var page = new net.hasor.dataway.web.WebRequest("GET", "/tools/console/", null, Map.of(), java.io.InputStream.nullInputStream(), null);
            String html = new String(ui.handle(page).body(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(html.contains("content=\"../../ops/manage/\""));
            assertTrue(html.contains("content=\"../../open/v2/\""));
            var large = new net.hasor.dataway.web.WebRequest("POST", "/open/v2/anything", null, Map.of("Content-Type", "application/json"), new java.io.ByteArrayInputStream(new byte[1025]), null);
            assertEquals(413, api.handle(large).status());
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
    }
}
