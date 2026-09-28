/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.hasor.cobble.setting.DefaultSettings;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.ApiBinder;
import net.hasor.core.Hasor;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.*;
import net.hasor.web.HandlerInterceptor;
import net.hasor.web.Invoker;
import net.hasor.web.WebApiBinder;
import net.hasor.web.binder.MappingDef;
import net.hasor.web.binder.OneConfig;
import net.hasor.web.invoker.InvokerContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatawayModuleTest {
    @Test
    void commonCoreSetupCanBePassedAsBuilderOrAssembledInstance() throws Throwable {
        for (boolean assembled : new boolean[] { false, true }) {
            var builder = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).configureQuery(runtime -> runtime.addShareVar("frameworkName", () -> (Udf) (hints, args) -> "hasor-spi"));
            Dataway prepared = assembled ? new Dataway(builder) : null;
            var module = assembled ? new DatawayModule(prepared) : new DatawayModule(builder);
            try (var context = Hasor.create().loadSettings(enabledProperties()).build(module)) {
                Dataway dataway = context.getInstance(Dataway.class);
                if (assembled) {
                    assertSame(prepared, dataway);
                }
                var service = dataway;
                ApiDefinition spiApi = new ApiDefinition();
                spiApi.setId("spi");
                spiApi.setMethod("GET");
                spiApi.setPath("/spi");
                spiApi.setType(ApiScriptType.DATA_QL);
                spiApi.setScript("return frameworkName();");
                spiApi.setDescription("");
                service.getAdminService().save(spiApi, 0);
                service.getAdminService().publish("spi", 1);
                assertEquals("hasor-spi", ((Map<?, ?>) this.execute(service, spiApi)).get("value"));
            }
        }
    }

    private Object execute(Dataway dataway, ApiDefinition definition) throws Throwable {
        var controller = new DatawayController("/api", dataway.getApiHandler());
        return JsonUtils.readValue(get(controller, "/api" + definition.getPath()), Object.class);
    }

    @Test
    void actualHasorContainerInstallsSuppliedCore() throws Throwable {
        var runtime = new DatawayConfig();
        var service = new Dataway(runtime.dataAccessLayer(TestDatabase.dataAccessLayer()));
        ApiDefinition helloApi = new ApiDefinition();
        helloApi.setId("hello");
        helloApi.setMethod("GET");
        helloApi.setPath("/hello");
        helloApi.setType(ApiScriptType.DATA_QL);
        helloApi.setScript("return 'hasor';");
        helloApi.setDescription("");
        service.getAdminService().save(helloApi, 0);
        service.getAdminService().publish("hello", 1);
        try (var context = Hasor.create().loadSettings(enabledProperties()).build(new DatawayModule(service))) {
            assertSame(service, context.getInstance(Dataway.class));
            assertEquals("hasor", ((Map<?, ?>) this.execute(context.getInstance(Dataway.class), helloApi)).get("value"));
        }
        // Closing the adapter does not own or close the supplied runtime.
        assertEquals("hasor", ((Map<?, ?>) this.execute(service, helloApi)).get("value"));
    }

    @Test
    void registeredMvcControllerUsesConfiguredMappingAndTranslatesContextPath() throws Throwable {
        var service = new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
        ApiDefinition helloApi = new ApiDefinition();
        helloApi.setId("hello");
        helloApi.setMethod("GET");
        helloApi.setPath("/hello");
        helloApi.setType(ApiScriptType.DATA_QL);
        helloApi.setScript("return ${name};");
        helloApi.setDescription("");
        service.getAdminService().save(helloApi, 0);
        service.getAdminService().publish("hello", 1);
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
            return method.getReturnType() == boolean.class ? false : null;
        });

        var invoker = proxy(Invoker.class, (p, method, args) -> switch (method.getName()) {
            case "getHttpRequest" -> request;
            case "getHttpResponse" -> response;
            default -> null;
        });
        captured.get().execute(invoker);
        assertEquals(200, status.get());
        assertEquals("Ada", JsonUtils.readTree(output.toString(StandardCharsets.UTF_8)).get("value").asText());
    }

    @Test
    void enabledCoreEntriesHaveSeparateMappings() throws Throwable {
        var service = new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
        for (int mask = 0; mask < 8; mask++) {
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            boolean docs = (mask & 4) != 0;
            DefaultSettings settings = new DefaultSettings();
            settings.setSetting("dataway.api-enabled", api);
            settings.setSetting("dataway.admin-enabled", admin);
            settings.setSetting("dataway.docs-enabled", docs);
            settings.setSetting("dataway.docs-prefix", "/specifications");
            settings.setSetting("dataway.api-prefix", "/open/v2");
            settings.setSetting("dataway.admin-prefix", "/ops/manage");
            settings.setSetting("dataway.admin-ui", "/tools/console");
            var bindings = new HashMap<Class<?>, Object>();
            var filters = new LinkedHashMap<String, DatawayController>();
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
                    if (paths[0].endsWith("swagger2.json")) {
                        assertArrayEquals(new String[] { "/specifications/swagger2.json", "/specifications/openapi.json" }, paths);
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
            new DatawayModule(service).loadModule(binder);
            {
                Dataway assembled = (Dataway) bindings.get(Dataway.class);
                assertSame(service, assembled);
                assertNotNull(assembled.getApiHandler());
                assertNotNull(assembled.getAdminHandler());
                assertNotNull(assembled.getAdminUiHandler());
            }
            assertFalse(bindings.containsKey(BeanContainer.class));
            assertEquals(api, filters.containsKey("/open/v2"));
            assertEquals(admin, filters.containsKey("/ops/manage"));
            assertEquals(admin, filters.containsKey("/tools/console"));
            assertEquals(docs, filters.containsKey("/specifications/swagger2.json"));
            assertEquals((api ? 1 : 0) + (admin ? 2 : 0) + (docs ? 1 : 0), filters.size());
        }
    }

    @Test
    void enablingManagementAlsoProvidesItsResources() throws Throwable {
        var settings = new Properties();
        settings.setProperty("dataway.admin-enabled", "true");
        settings.setProperty("dataway.admin-ui", "/console");
        var builder = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer());
        try (var context = Hasor.create().loadSettings(settings).build(new DatawayModule(builder))) {
            Dataway dataway = context.getInstance(Dataway.class);
            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway);
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getAdminUiHandler());
        }
    }

    @Test
    void nativeConfigurationFileConfiguresHandlersAndHostMappings() throws Throwable {
        JdbcDataSource source = dataSource();
        Set<Operation> authorized = EnumSet.noneOf(Operation.class);
        var identity = UserIdentity.authenticated("hasor-user");
        var builder = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(source, "")).identityProvider(request -> identity).authorizationCheck((user, operation) -> {
            assertSame(identity, user);
            authorized.add(operation);
            return true;
        });
        try (var context = Hasor.create().mainSettingWith("configured/hconfig.xml").build(new DatawayModule(builder))) {
            Dataway dataway = context.getInstance(Dataway.class);

            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getAdminUiHandler());

            ApiDefinition storedApi = new ApiDefinition();
            storedApi.setId("stored");
            storedApi.setMethod("GET");
            storedApi.setPath("/stored");
            storedApi.setType(ApiScriptType.DATA_QL);
            storedApi.setScript("return 1;");
            storedApi.setDescription("");
            dataway.getAdminService().save(storedApi, 0);
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            var filters = mountedControllers(dataway, context.getSettings());
            assertEquals(Set.of("/open/v2", "/ops/manage", "/tools/console", "/specifications/swagger2.json"), filters.keySet());
            String html = get(filters.get("/tools/console"), "/tools/console/");
            assertFalse(html.contains("dataway-admin-api"));
            assertFalse(html.contains("dataway-api"));
            dataway.getAdminService().publish("stored", 1);
            assertEquals(1, JsonUtils.readTree(get(filters.get("/open/v2"), "/open/v2/stored")).get("value").asInt());
            assertTrue(get(filters.get("/ops/manage"), "/ops/manage/api-list").contains("stored"));
            String document = get(filters.get("/specifications/swagger2.json"), "/specifications/openapi.json");
            assertTrue(JsonUtils.readTree(document).path("paths").has("/stored"));
            assertEquals(Set.of(Operation.INVOKE, Operation.LIST, Operation.DOCUMENT), authorized);
        }
    }

    @Test
    void omittedSettingsAndCompleteDefaultFileLeaveEveryEntryInactive() throws Throwable {
        for (boolean fromFile : new boolean[] { false, true }) {
            var host = Hasor.create();
            if (fromFile) {
                host.mainSettingWith("defaults/hconfig.xml");
            }
            var builder = new DatawayConfig().configureHost(hostConfiguration -> {
                throw new AssertionError("Disabled entries must not initialize a runtime");
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
            properties.setProperty("dataway.docs-enabled", Boolean.toString(enabled));
            try (var context = Hasor.create().mainSettingWith("configured/hconfig.xml").loadSettings(properties).build(new DatawayModule(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer())))) {
                Dataway dataway = context.findBindingBean(null, Dataway.class);

                if (enabled) {
                    ApiDefinition configuredApi = new ApiDefinition();
                    configuredApi.setId("configured");
                    configuredApi.setMethod("GET");
                    configuredApi.setPath("/configured");
                    configuredApi.setType(ApiScriptType.DATA_QL);
                    configuredApi.setScript("return 1;");
                    configuredApi.setDescription("");
                    dataway.getAdminService().save(configuredApi, 0);
                    assertEquals(1, dataway.getAdminService().list().size());
                    assertNotNull(dataway.getApiHandler());
                    assertNotNull(dataway.getAdminHandler());
                    assertNotNull(dataway.getAdminUiHandler());
                } else {
                    assertNull(dataway);
                }
            }
        }
    }

    @Test
    void allHasorEntriesResolveHostIdentityThroughProvider() throws Throwable {
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
        var builder = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            assertFalse(request.getIdentity().authenticated());
            Object attribute = request.getAttribute("host.user");
            UserIdentity identity = attribute instanceof UserIdentity user ? user : UserIdentity.anonymous();
            identities.put(request.getPath(), identity);
            return identity;
        });
        Dataway dataway = builder.createDataway();
        this.publish(dataway, "identity", "/", "return 1;");
        new DatawayModule(dataway).loadModule(binder);
        UserIdentity explicit = new UserIdentity("attribute-user", true, Map.of("tenant", "one"));
        for (String prefix : List.of("/api", "/dataway/api", "/dataway")) {
            String path = prefix.equals("/dataway/api") ? prefix + "/api-list" : prefix + "/";
            for (boolean attribute : new boolean[] { false, true }) {
                var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
                    case "getRequestURI" -> "/host" + path;
                    case "getContextPath" -> "/host";
                    case "getMethod" -> "GET";
                    case "getHeaderNames" -> Collections.emptyEnumeration();
                    case "getUserPrincipal" -> {
                        throw new AssertionError("The adapter must delegate identity resolution to IdentityProvider");
                    }
                    case "getAttribute" -> attribute && "host.user".equals(args[0]) ? explicit : null;
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
                UserIdentity actual = identities.get(path);
                assertEquals(attribute, actual.authenticated());
                if (attribute) {
                    assertSame(explicit, actual);
                }
            }
        }
    }

    private void publish(Dataway dataway, String id, String path, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("GET");
        definition.setPath(path);
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("");
        dataway.getAdminService().save(definition, 0);
        dataway.getAdminService().publish(id, 1);
    }

    @Test
    void uiRemainsStaticAcrossServletContextsAndPrefixes() throws Throwable {
        Dataway dataway = new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
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
            String result = get(filters.get("/ops/manage.v2"), "/ops/manage.v2/api-list", contextPath);
            assertTrue(result.contains("\"result\":[]"));
        }
    }

    @Test
    void actualHasorMvcChainRunsHostInterceptorBeforeAllControllers() throws Throwable {
        var attributes = new HashMap<String, Object>();
        var servlet = proxy(ServletContext.class, (p, method, args) -> switch (method.getName()) {
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
        AtomicInteger handled = new AtomicInteger();
        AtomicBoolean failRequests = new AtomicBoolean();
        DatawayException failure = new DatawayException(401, "Denied by host policy");
        var builder = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            assertEquals("host-user", request.getAttribute("host.user"));
            if (failRequests.get()) {
                throw failure;
            }
            return UserIdentity.authenticated("host-user");
        }).apiInterceptor((invocation, next) -> new byte[] { 1 }).adminInterceptor((invocation, next) -> ResultInfoUtils.convertToResultInfo("application/octet-stream", new byte[] { 2 }));
        try (var application = Hasor.create(servlet).loadSettings(enabledProperties()).build(binder -> {
            WebApiBinder web = binder.tryCast(WebApiBinder.class);
            assertNotNull(web);
            web.addExceptionHandler(DatawayException.class, (invoker, error) -> {
                assertSame(failure, error);
                handled.incrementAndGet();
                invoker.getHttpResponse().setStatus(error.status());
                return Map.of("message", "Handled by the host");
            });
            web.bindInterceptor(new HandlerInterceptor() {
                @Override
                public boolean preHandle(Invoker invoker) {
                    assertNotNull(invoker.ownerMapping());
                    invoker.getHttpRequest().setAttribute("host.user", "host-user");
                    intercepted.incrementAndGet();
                    return true;
                }
            });
        }, new DatawayModule(builder))) {
            this.publish(application.getInstance(Dataway.class), "custom", "/custom/example", "return 1;");
            var mappings = application.findBindingBean(MappingDef.class);
            for (String unmapped : List.of("/api-other", "/dataway-other", "/outside")) {
                var unmatched = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
                    case "getRequestURI" -> "/host" + unmapped;
                    case "getContextPath" -> "/host";
                    case "getMethod" -> "GET";
                    default -> null;
                });
                assertFalse(mappings.stream().anyMatch(mapping -> mapping.matchingMapping(unmatched)));
            }
            var mvc = new InvokerContext();
            mvc.initContext(application, new OneConfig("test", () -> application));
            try {
                int expected = 0;
                for (String path : List.of("/api/custom/example", "/dataway/api/api-list", "/dataway/", "/api/custom/example", "/dataway/api/api-list", "/dataway/")) {
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
                        case "getInputStream" -> emptyBody();
                        default -> null;
                    });
                    var output = new ByteArrayOutputStream();
                    var status = new AtomicInteger(200);
                    var response = proxy(HttpServletResponse.class, (p, method, args) -> {
                        if (method.getName().equals("setStatus")) {
                            status.set((Integer) args[0]);
                        }
                        if (method.getName().equals("getStatus")) {
                            return status.get();
                        }
                        if (method.getName().equals("getCharacterEncoding")) {
                            return "UTF-8";
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
                    mvc.genCaller(request, response).invoke((input, result) -> fail("MVC mapping must select Dataway")).get();
                    if (failRequests.get()) {
                        assertEquals(401, status.get());
                        assertEquals("{\"message\":\"Handled by the host\"}", output.toString(StandardCharsets.UTF_8));
                    } else {
                        expected++;
                        if (expected < 3) {
                            assertArrayEquals(new byte[] { (byte) expected }, output.toByteArray());
                        } else {
                            assertTrue(output.toString(StandardCharsets.UTF_8).contains("<div id=\"app\""));
                        }
                    }
                    if (expected == 3) {
                        failRequests.set(true);
                    }
                }
                assertEquals(6, intercepted.get());
                assertEquals(3, handled.get());
            } finally {
                mvc.destroyContext();
            }
        }
    }

    @Test
    void controllerPreservesRepeatedHeadersAndCookies() throws Exception {
        Map<String, List<String>> input = Map.of("X-Repeat", List.of("one", "two"), "Cookie", List.of("id=first; id=second"));
        var request = proxy(HttpServletRequest.class, (p, method, args) -> switch (method.getName()) {
            case "getRequestURI" -> "/host/api/cookies";
            case "getContextPath" -> "/host";
            case "getMethod" -> "GET";
            case "getHeaderNames" -> Collections.enumeration(input.keySet());
            case "getHeaders" -> Collections.enumeration(input.get(args[0]));
            case "getInputStream" -> emptyBody();
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
        var config = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).resultStructure(false).apiInterceptor((invocation, next) -> {
            next.proceed(invocation);
            return new byte[] { 42 };
        }).identityProvider(webRequest -> {
            assertEquals("/cookies", webRequest.getPathInfo());
            assertEquals(List.of("one", "two"), webRequest.getHeaderValues().get("x-repeat"));
            assertEquals(List.of("first", "second"), webRequest.getCookies().get("id"));
            return UserIdentity.anonymous();
        });
        var dataway = config.createDataway();
        this.publish(dataway, "cookies", "/cookies", """
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                run web.setHeader('X-Result', 'first');
                run web.addHeader('x-result', 'second');
                run web.setCookie('one', '1');
                run web.setCookie('two', '2');
                return 1;
                """);
        var controller = new DatawayController("/api", dataway.getApiHandler());
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

    private static ServletInputStream emptyBody() {
        return new ServletInputStream() {
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
    }

    private static Properties enabledProperties() {
        var settings = new Properties();
        settings.setProperty("dataway.api-enabled", "true");
        settings.setProperty("dataway.admin-enabled", "true");
        return settings;
    }

    private static DefaultSettings enabledSettings() throws IOException {
        var settings = new DefaultSettings();
        settings.setSetting("dataway.api-enabled", true);
        settings.setSetting("dataway.admin-enabled", true);
        return settings;
    }

    private static JdbcDataSource dataSource() {
        return TestDatabase.create();
    }

    private static ServletContext servletContext(String contextPath) {
        return proxy(ServletContext.class, (p, method, args) -> {
            if (method.getName().equals("getContextPath")) {
                return contextPath;
            }
            return null;
        });
    }

    private static Map<String, DatawayController> mountedControllers(Dataway dataway, Settings settings) {
        return mountedControllers(dataway, settings, "/host");
    }

    private static Map<String, DatawayController> mountedControllers(Dataway dataway, Settings settings, String contextPath) {
        Map<String, DatawayController> filters = new LinkedHashMap<>();
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

    static String get(DatawayController filter, String path) throws Throwable {
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
        return output.toString(StandardCharsets.UTF_8);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
    }
}
