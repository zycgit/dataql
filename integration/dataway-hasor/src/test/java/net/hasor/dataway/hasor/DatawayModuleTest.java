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
import java.util.*;
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
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.FxRuntime;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayConfigurer;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.web.Invoker;
import net.hasor.web.WebApiBinder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatawayModuleTest {
    @Test
    void commonCoreSetupCanBePassedAsBuilderOrAssembledInstance() throws Throwable {
        DatawayConfigurer setup = builder -> builder.dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).configureRuntime(runtime -> runtime.function("frameworkName", (hints, args) -> "hasor-spi"));
        for (boolean assembled : new boolean[] { false, true }) {
            var builder = Dataway.builder().configure(setup);
            Dataway prepared = assembled ? builder.build() : null;
            var module = assembled ? new DatawayModule(prepared) : new DatawayModule(builder);
            try (var context = Hasor.create().loadSettings(enabledProperties()).build(module)) {
                Dataway dataway = context.getInstance(Dataway.class);
                if (assembled) {
                    assertSame(prepared, dataway);
                }
                var service = dataway.getService();
                service.save(new ApiDefinition("spi", "GET", "/spi", ScriptType.DATAQL, "return frameworkName();", ""), 0, CallContext.LOCAL);
                service.publish("spi", 1, CallContext.LOCAL);
                assertEquals("hasor-spi", service.invokeApi("/spi", Map.of()));
            }
        }
    }

    @Test
    void actualHasorContainerInstallsIndependentService() throws Throwable {
        var runtime = FxRuntime.builder().build();
        var service = DatawayService.builder(runtime, TestDatabase.dataAccessLayer()).build();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return 'hasor';", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        try (var context = Hasor.create().loadSettings(enabledProperties()).build(new DatawayModule(Dataway.builder().service(service)))) {
            assertSame(service, context.getInstance(Dataway.class).getService());
            assertEquals("hasor", context.getInstance(Dataway.class).getService().invokeApi("/hello", Map.of()));
        }
        // Closing the adapter does not own or close the supplied runtime.
        assertEquals("hasor", service.invokeApi("/hello", Map.of()));
    }

    @Test
    void registeredMvcControllerUsesConfiguredMappingAndTranslatesContextPath() throws Throwable {
        var service = DatawayService.builder(FxRuntime.builder().build(), TestDatabase.dataAccessLayer()).build();
        service.save(new ApiDefinition("hello", "GET", "/hello", ScriptType.DATAQL, "return ${name};", ""), 0, CallContext.LOCAL);
        service.publish("hello", 1, CallContext.LOCAL);
        var settings = new DefaultSettings();
        settings.setSetting("dataway.api-prefix", "/open/v2");
        settings.setSetting("dataway.api-enabled", true);
        AtomicReference<DatawayController> captured = new AtomicReference<>();
        // Capture the actual controller registered through the public Hasor binder contract.
        WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
            case "tryCast" -> p;
            case "getServletContext" -> servletContext("/host");
            case "getSettings" -> settings;
            case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> null);
            case "mappingTo" -> {
                assertArrayEquals(new String[] { "/open/v2", "/open/v2/*" }, (String[]) args[0]);
                yield proxy(WebApiBinder.MappingToBindingBuilder.class, (x, m, a) -> {
                    for (Object value : a) {
                        if (value instanceof DatawayController filter) {
                            captured.set(filter);
                        }
                    }
                    return null;
                });
            }
            default -> null;
        });

        new DatawayModule(Dataway.builder().service(service)).loadModule(binder);
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
            return method.getReturnType() == boolean.class ? false : null;
        });

        var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
            case "getHttpRequest" -> request;
            case "getHttpResponse" -> response;
            default -> null;
        });
        captured.get().execute(invoker);
        assertEquals(200, status.get());
        assertEquals("\"Ada\"", output.toString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void enabledCoreEntriesHaveSeparateMappings() throws Throwable {
        var service = DatawayService.builder(FxRuntime.builder().build(), TestDatabase.dataAccessLayer()).build();
        for (int mask = 0; mask < 4; mask++) {
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            DefaultSettings settings = new DefaultSettings();
            settings.setSetting("dataway.api-enabled", api);
            settings.setSetting("dataway.admin-enabled", admin);
            settings.setSetting("dataway.api-prefix", "/open/v2");
            settings.setSetting("dataway.admin-prefix", "/ops/manage");
            settings.setSetting("dataway.admin-ui", "/tools/console");
            var bindings = new java.util.HashMap<Class<?>, Object>();
            var filters = new java.util.LinkedHashMap<String, DatawayController>();
            WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
                case "tryCast" -> p;
                case "getServletContext" -> servletContext("/host");
                case "getSettings" -> settings;
                case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> {
                    if (m.getName().equals("toInstance")) {
                        bindings.put((Class<?>) args[0], a[0]);
                    }
                    return null;
                });
                case "mappingTo" -> {
                    String[] paths = (String[]) args[0];
                    if (paths[0].equals("/tools/console")) {
                        assertArrayEquals(new String[] { "/tools/console", "/tools/console/", "/tools/console/assets/app.js", "/tools/console/assets/app.css", "/tools/console/config.json" }, paths);
                    } else {
                        assertArrayEquals(new String[] { paths[0], paths[0] + "/*" }, paths);
                    }
                    yield proxy(WebApiBinder.MappingToBindingBuilder.class, (x, m, a) -> {
                        for (Object value : a) {
                            if (value instanceof DatawayController filter) {
                                filters.put(paths[0], filter);
                            }
                        }
                        return null;
                    });
                }
                default -> null;
            });
            new DatawayModule(Dataway.builder().service(service)).loadModule(binder);
            if (api || admin) {
                Dataway assembled = (Dataway) bindings.get(Dataway.class);
                assertSame(service, assembled.getService());
                assertNotNull(assembled.getApiHandler());
                assertNotNull(assembled.getAdminHandler());
                assertNotNull(assembled.getUiHandler());
            } else {
                assertFalse(bindings.containsKey(Dataway.class));
            }
            assertFalse(bindings.containsKey(DatawayService.class));
            assertEquals(api, filters.containsKey("/open/v2"));
            assertEquals(admin, filters.containsKey("/ops/manage"));
            assertEquals(admin, filters.containsKey("/tools/console"));
            assertEquals((api ? 1 : 0) + (admin ? 2 : 0), filters.size());
        }
    }

    @Test
    void enablingManagementAlsoProvidesItsResources() throws Throwable {
        var settings = new java.util.Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        settings.setProperty("dataway.admin-ui", "/console");
        var builder = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer());
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(builder))) {
            Dataway dataway = context.getInstance(Dataway.class);
            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getService());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getUiHandler());
        }
    }

    @Test
    void nativeConfigurationFileConfiguresHandlersAndHostMappings() throws Throwable {
        JdbcDataSource source = dataSource();
        var builder = Dataway.builder().dataSource(source).dataAccessLayer(new net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer(source, ""));
        try (var context = Hasor.create().mainSettingWith("configured/hconfig.xml").build(new DatawayModule(builder))) {
            Dataway dataway = context.getInstance(Dataway.class);

            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getUiHandler());

            dataway.getService().save(new ApiDefinition("stored", "GET", "/stored", ScriptType.DATAQL, "return 1;", ""), 0, CallContext.LOCAL);
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            var filters = mountedControllers(dataway, context.getSettings());
            assertEquals(Set.of("/open/v2", "/ops/manage", "/tools/console"), filters.keySet());
            String html = get(filters.get("/tools/console"), "/tools/console/");
            assertFalse(html.contains("dataway-admin-api"));
            assertFalse(html.contains("dataway-api"));

        }
    }

    @Test
    void omittedSettingsAndCompleteDefaultFileLeaveEveryEntryInactive() throws Throwable {
        for (boolean fromFile : new boolean[] { false, true }) {
            var host = Hasor.create();
            if (fromFile) {
                host.mainSettingWith("defaults/hconfig.xml");
            }
            var builder = Dataway.builder().dataSource(() -> {
                throw new AssertionError("Disabled entries must not resolve a datasource");
            });
            try (var context = host.build(new DatawayModule(builder))) {
                assertTrue(context.findBindingRegister(Dataway.class).isEmpty());
            }
        }
    }

    @Test
    void eachEntryCanBeExplicitlyDisabledDespiteTheEnabledConfigurationFile() throws Throwable {
        for (boolean enabled : new boolean[] { true, false }) {
            Properties properties = new Properties();
            properties.setProperty("dataway.api-enabled", Boolean.toString(enabled));
            properties.setProperty("dataway.admin-enabled", Boolean.toString(enabled));
            try (var context = Hasor.create().mainSettingWith("configured/hconfig.xml").loadSettings(properties).build(new DatawayModule(Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer())))) {
                Dataway dataway = context.findBindingBean(null, Dataway.class);

                if (enabled) {
                    dataway.getService().save(new ApiDefinition("configured", "GET", "/configured", ScriptType.DATAQL, "return 1;", ""), 0, CallContext.LOCAL);
                    assertEquals(1, dataway.getService().list(CallContext.LOCAL).size());
                    assertNotNull(dataway.getApiHandler());
                    assertNotNull(dataway.getAdminHandler());
                    assertNotNull(dataway.getUiHandler());
                } else {
                    assertNull(dataway);
                }
            }
        }
    }

    @Test
    void allHasorEntriesAcceptHostIdentityAttributesAndPrincipalFallback() throws Throwable {
        var filters = new LinkedHashMap<String, DatawayController>();
        var identities = new LinkedHashMap<String, UserIdentity>();
        WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
            case "tryCast" -> p;
            case "getServletContext" -> servletContext("/host");
            case "getSettings" -> enabledSettings();
            case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> null);
            case "mappingTo" -> proxy(WebApiBinder.MappingToBindingBuilder.class, (x, m, a) -> {
                for (Object value : a) {
                    if (value instanceof DatawayController filter) {
                        filters.put(((String[]) args[0])[0], filter);
                    }
                }
                return null;
            });
            default -> null;
        });
        var builder = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            identities.put(request.getPath(), request.getIdentity());
            return request.getIdentity();
        });
        builder.apiHandler(core -> (request, response) -> response.write(200, Map.of()));
        builder.adminHandler(core -> (request, response) -> response.write(200, Map.of()));
        builder.uiHandler(core -> (request, response) -> response.write(200, Map.of()));
        new DatawayModule(builder).loadModule(binder);
        UserIdentity explicit = new UserIdentity("attribute-user", true, Map.of("tenant", "one"));
        for (String prefix : List.of("/api", "/dataway/api", "/dataway")) {
            for (boolean attribute : new boolean[] { false, true }) {
                var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
                    case "getRequestURI" -> "/host" + prefix + "/";
                    case "getContextPath" -> "/host";
                    case "getMethod" -> "GET";
                    case "getHeaderNames" -> Collections.emptyEnumeration();
                    case "getUserPrincipal" -> (java.security.Principal) () -> "principal-user";
                    case "getAttribute" -> attribute && RequestAttribute.IDENTITY.getKey().equals(args[0]) ? explicit : null;
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
                var response = proxy(HttpServletResponse.class, (p, method, args) -> {
                    if (method.getName().equals("getOutputStream")) {
                        return new ServletOutputStream() {
                            public void write(int value) {
                            }

                            public boolean isReady() {
                                return true;
                            }

                            public void setWriteListener(WriteListener listener) {
                            }
                        };
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
                var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
                    case "getHttpRequest" -> request;
                    case "getHttpResponse" -> response;
                    default -> null;
                });
                filters.get(prefix).execute(invoker);
                UserIdentity actual = identities.get(prefix + "/");
                assertTrue(actual.isAuthenticated());
                assertEquals(attribute ? "attribute-user" : "principal-user", actual.getId());
                if (attribute) {
                    assertSame(explicit, actual);
                }
            }
        }
    }

    @Test
    void uiRemainsStaticAcrossServletContextsAndPrefixes() throws Throwable {
        Dataway dataway = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).build();
        var settings = enabledSettings();
        settings.setSetting("dataway.api-prefix", "/v1.0");
        settings.setSetting("dataway.admin-prefix", "/ops/manage.v2");
        settings.setSetting("dataway.admin-ui", "/console.v2");
        for (String contextPath : new String[] { "", "/tenant/host" }) {
            var filters = mountedControllers(dataway, settings, contextPath);
            String pagePath = "/console.v2/";
            String html = get(filters.get("/console.v2"), pagePath, contextPath);
            assertFalse(html.contains("dataway-admin-api"));
            assertFalse(html.contains("dataway-api"));
            String result = get(filters.get("/ops/manage.v2"), "/ops/manage.v2/apis", contextPath);
            assertEquals("[]", result);
        }
    }

    @Test
    void actualHasorMvcChainRunsHostInterceptorBeforeAllControllers() throws Throwable {
        var attributes = new HashMap<String, Object>();
        var servlet = proxy(javax.servlet.ServletContext.class, (p, method, args) -> switch (method.getName()) {
            case "getContextPath" -> "/host";
            case "getClassLoader" -> getClass().getClassLoader();
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> attributes.put((String) args[0], args[1]);
            case "getAttributeNames", "getInitParameterNames" -> Collections.emptyEnumeration();
            case "getEffectiveMajorVersion", "getMajorVersion" -> 4;
            case "getEffectiveMinorVersion", "getMinorVersion" -> 0;
            case "getVirtualServerName" -> "test";
            default -> null;
        });
        AtomicInteger intercepted = new AtomicInteger();
        var builder = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            assertEquals("host-user", request.getAttribute("host.user"));
            return UserIdentity.authenticated("host-user");
        });
        builder.apiHandler(core -> (request, response) -> response.write(200, Map.of()).write(new byte[] { 1 }));
        builder.adminHandler(core -> (request, response) -> response.write(200, Map.of()).write(new byte[] { 2 }));
        builder.uiHandler(core -> (request, response) -> response.write(200, Map.of()).write(new byte[] { 3 }));
        try (var application = Hasor.create(servlet).loadSettings(enabledProperties()).build(binder -> {
            WebApiBinder web = binder.tryCast(WebApiBinder.class);
            assertNotNull(web);
            web.filter("/*").through((invoker, chain) -> {
                assertNotNull(invoker.ownerMapping());
                invoker.getHttpRequest().setAttribute("host.user", "host-user");
                intercepted.incrementAndGet();
                return chain.doNext(invoker);
            });
        }, new DatawayModule(builder))) {
            var mvc = new net.hasor.web.invoker.InvokerContext();
            mvc.initContext(application, new net.hasor.web.binder.OneConfig("test", () -> application));
            try {
                int expected = 0;
                for (String path : List.of("/api/example", "/dataway/api/apis", "/dataway/assets/app.js")) {
                    var requestAttributes = new HashMap<String, Object>();
                    var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
                        case "getRequestURI" -> "/host" + path;
                        case "getContextPath" -> "/host";
                        case "getMethod" -> "GET";
                        case "getCharacterEncoding" -> "UTF-8";
                        case "getHeaderNames", "getAttributeNames", "getParameterNames" -> Collections.emptyEnumeration();
                        case "getParameterMap" -> Map.of();
                        case "getAttribute" -> requestAttributes.get(args[0]);
                        case "setAttribute" -> requestAttributes.put((String) args[0], args[1]);
                        case "isAsyncSupported", "isAsyncStarted" -> false;
                        case "getInputStream" -> throw new AssertionError("Custom entry must not acquire body");
                        default -> null;
                    });
                    var output = new ByteArrayOutputStream();
                    var response = proxy(HttpServletResponse.class, (p, method, args) -> {
                        if (method.getName().equals("getOutputStream")) {
                            return new ServletOutputStream() {
                                public void write(int value) {
                                    output.write(value);
                                }

                                public boolean isReady() {
                                    return true;
                                }

                                public void setWriteListener(WriteListener listener) {
                                }
                            };
                        }
                        return method.getReturnType() == boolean.class ? false : null;
                    });
                    mvc.genCaller(request, response).invoke((input, result) -> fail("MVC mapping must select Dataway")).get();
                    assertArrayEquals(new byte[] { (byte) ++expected }, output.toByteArray());
                }
                assertEquals(3, intercepted.get());
            } finally {
                mvc.destroyContext();
            }
        }
    }

    @Test
    void controllerPreservesRepeatedHeadersAndCookiesWithoutReadingBody() throws Exception {
        Map<String, List<String>> input = Map.of("X-Repeat", List.of("one", "two"), "Cookie", List.of("id=first; id=second"));
        var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
            case "getRequestURI" -> "/host/api/cookies";
            case "getContextPath" -> "/host";
            case "getMethod" -> "GET";
            case "getHeaderNames" -> Collections.enumeration(input.keySet());
            case "getHeaders" -> Collections.enumeration(input.get(args[0]));
            case "getInputStream" -> throw new AssertionError("Header and cookie access must not read the body");
            default -> null;
        });
        Map<String, List<String>> outputHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        outputHeaders.put("Set-Cookie", new ArrayList<>(List.of("host=existing")));
        var output = new ByteArrayOutputStream();
        var response = proxy(HttpServletResponse.class, (p, method, args) -> {
            if (method.getName().equals("setHeader")) {
                outputHeaders.put((String) args[0], new ArrayList<>(List.of((String) args[1])));
            } else if (method.getName().equals("addHeader")) {
                outputHeaders.computeIfAbsent((String) args[0], key -> new ArrayList<>()).add((String) args[1]);
            } else if (method.getName().equals("getOutputStream")) {
                return new ServletOutputStream() {
                    public void write(int value) {
                        output.write(value);
                    }

                    public boolean isReady() {
                        return true;
                    }

                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
            return method.getReturnType() == boolean.class ? false : null;
        });
        var controller = new DatawayController("/api", (webRequest, webResponse) -> {
            assertEquals("/cookies", webRequest.getPathInfo());
            assertEquals(List.of("one", "two"), webRequest.getHeaderValues().get("x-repeat"));
            assertEquals(List.of("first", "second"), webRequest.getCookies().get("id"));
            webResponse.setHeader("X-Result", "first");
            webResponse.addHeader("x-result", "second");
            webResponse.setCookie(new net.hasor.dataway.web.WebCookie("one", "1"));
            webResponse.setCookie(new net.hasor.dataway.web.WebCookie("two", "2"));
            webResponse.write(200, Map.of()).write(42);
        });
        var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
            case "getHttpRequest" -> request;
            case "getHttpResponse" -> response;
            default -> null;
        });
        controller.execute(invoker);
        assertEquals(List.of("first", "second"), outputHeaders.get("X-Result"));
        assertEquals(List.of("host=existing", "one=1; Path=/", "two=2; Path=/"), outputHeaders.get("Set-Cookie"));
        assertArrayEquals(new byte[] { 42 }, output.toByteArray());
    }

    private static Properties enabledProperties() {
        var settings = new Properties();
        settings.setProperty("dataway.api-enabled", "true");
        settings.setProperty("dataway.admin-enabled", "true");
        return settings;
    }

    private static DefaultSettings enabledSettings() throws java.io.IOException {
        var settings = new DefaultSettings();
        settings.setSetting("dataway.api-enabled", true);
        settings.setSetting("dataway.admin-enabled", true);
        return settings;
    }

    private static JdbcDataSource dataSource() {
        return TestDatabase.create();
    }

    private static javax.servlet.ServletContext servletContext(String contextPath) {
        return proxy(javax.servlet.ServletContext.class, (p, method, args) -> {
            if (method.getName().equals("getContextPath")) {
                return contextPath;
            }
            return null;
        });
    }

    private static Map<String, DatawayController> mountedControllers(Dataway dataway, net.hasor.cobble.setting.Settings settings) {
        return mountedControllers(dataway, settings, "/host");
    }

    private static Map<String, DatawayController> mountedControllers(Dataway dataway, net.hasor.cobble.setting.Settings settings, String contextPath) {
        Map<String, DatawayController> filters = new java.util.LinkedHashMap<>();
        WebApiBinder binder = proxy(WebApiBinder.class, (p, method, args) -> switch (method.getName()) {
            case "tryCast" -> p;
            case "getServletContext" -> servletContext(contextPath);
            case "getSettings" -> settings;
            case "bindType" -> proxy(ApiBinder.NamedBindingBuilder.class, (x, m, a) -> null);
            case "mappingTo" -> proxy(WebApiBinder.MappingToBindingBuilder.class, (x, m, a) -> {
                for (Object value : a) {
                    if (value instanceof DatawayController filter) {
                        filters.put(((String[]) args[0])[0], filter);
                    }
                }
                return null;
            });
            default -> null;
        });
        new DatawayModule(dataway).loadModule(binder);
        return filters;
    }

    private static String get(DatawayController filter, String path) throws Throwable {
        return get(filter, path, "/host");
    }

    private static String get(DatawayController filter, String path, String contextPath) throws Throwable {
        var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
            case "getRequestURI" -> contextPath + path;
            case "getContextPath" -> contextPath;
            case "getMethod" -> "GET";
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
        AtomicInteger status = new AtomicInteger();
        var response = proxy(HttpServletResponse.class, (p, method, args) -> {
            if (method.getName().equals("setStatus")) {
                status.set((Integer) args[0]);
            }
            if (method.getName().equals("getOutputStream")) {
                return new ServletOutputStream() {
                    public void write(int value) {
                        output.write(value);
                    }

                    public boolean isReady() {
                        return true;
                    }

                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
            return method.getReturnType() == boolean.class ? false : null;
        });
        var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
            case "getHttpRequest" -> request;
            case "getHttpResponse" -> response;
            default -> null;
        });
        filter.execute(invoker);
        assertEquals(200, status.get());
        return output.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
    }
}
