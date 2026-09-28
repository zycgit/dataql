/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.*;
import net.hasor.dataway.service.admin.AdminInterceptor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class DatawayAutoConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class));

    @Test
    void mvcMappingUsesTypeAndPrimaryInsteadOfARequiredBeanName() {
        var runner = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class)).withBean(Dataway.class, () -> new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()))).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").withBean("hostRoutes", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new);
        runner.run(c -> {
            assertNull(c.getStartupFailure());
            assertFalse(c.containsBean("requestMappingHandlerMapping"));
            assertEquals(3, mappings(c.getBean("hostRoutes", RequestMappingHandlerMapping.class)).size());
        });
        runner.withBean("primaryRoutes", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new, definition -> definition.setPrimary(true)).run(c -> {
            assertNull(c.getStartupFailure());
            assertTrue(mappings(c.getBean("hostRoutes", RequestMappingHandlerMapping.class)).isEmpty());
            assertEquals(3, mappings(c.getBean("primaryRoutes", RequestMappingHandlerMapping.class)).size());
        });
        runner.withBean("otherRoutes", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new).run(c -> {
            Throwable failure = c.getStartupFailure();
            assertNotNull(failure);
            while (failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertInstanceOf(NoUniqueBeanDefinitionException.class, failure);
        });
    }

    @Test
    void missingAccessLayerFailsWhenAnEntryIsEnabled() {
        context.withPropertyValues("dataway.admin-enabled=true").run(c -> {
            Throwable failure = c.getStartupFailure();
            assertNotNull(failure);
            while (failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertTrue(failure.getMessage().contains("ApiDataAccessLayer"));
        });
    }

    @Test
    void disabledDefaultsDoNotAssembleCoreOrRegisterMappings() throws Exception {
        for (boolean fromFile : new boolean[] { false, true }) {
            var runner = context.withBean(DatawayConfig.class, () -> new DatawayConfig().configureHost(host -> {
                throw new AssertionError("Disabled entries must not initialize a runtime");
            }));

            if (fromFile) {
                var defaults = configuration("defaults/application.yml");
                runner = runner.withInitializer(c -> c.getEnvironment().getPropertySources().addLast(defaults));
            }

            runner.run(c -> {
                assertNull(c.getStartupFailure());
                assertFalse(c.getSourceApplicationContext().getBeanFactory().containsSingleton("dataway"));
                assertTrue(mappings(c.getBean(RequestMappingHandlerMapping.class)).isEmpty());
            });
        }
        context.withPropertyValues("dataway.api-enabled=true").run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void configurationBeanAndSuppliedInstanceRemainSupported() {
        context.withBean(DatawayConfig.class, () -> new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer())).withPropertyValues("dataway.api-enabled=true").run(c -> {
            Dataway dataway = c.getBean(Dataway.class);
            publish(dataway, "return 'configured';");
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var response = mvc.perform(MockMvcRequestBuilders.get("/api/hello")).andReturn().getResponse();
            assertEquals("configured", JsonUtils.readTree(response.getContentAsString()).get("value").stringValue());
            assertTrue(c.getBeansOfType(BeanContainer.class).isEmpty());
            assertTrue(c.getBeansOfType(WebHandler.class).isEmpty());
            assertTrue(c.getBeansOfType(FilterRegistrationBean.class).isEmpty());
        });
        Dataway supplied = new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()));
        context.withBean(Dataway.class, () -> supplied).withBean(DatawayConfig.class, () -> new DatawayConfig().configureHost(host -> fail("Already assembled"))).withPropertyValues("dataway.api-enabled=true").run(c -> {
            assertNull(c.getStartupFailure());
            assertSame(supplied, c.getBean(Dataway.class));
            assertEquals(1, mappings(c.getBean(RequestMappingHandlerMapping.class)).size());
        });
    }

    @Test
    void allSwitchCombinationsUseConfiguredMvcMappings() throws Exception {
        var configured = configuration("configured/application.yml");
        for (int mask = 0; mask < 4; mask++) {
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            context.withBean("metadataStorage", DatawayConfig.class, () -> new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer())).withBean(DataSource.class, TestDatabase::create).withInitializer(c -> c.getEnvironment().getPropertySources().addLast(configured)).withPropertyValues("dataway.api-enabled=" + api, "dataway.admin-enabled=" + admin).run(c -> {
                assertNull(c.getStartupFailure());
                var paths = mappings(c.getBean(RequestMappingHandlerMapping.class));
                assertEquals((api ? 1 : 0) + (admin ? 2 : 0), paths.size());
                if (api) {
                    assertEquals(Set.of("/open/v2", "/open/v2/{*path}"), paths.get("datawayApi"));
                }
                if (admin) {
                    assertEquals(Set.of("/ops/manage", "/ops/manage/{*path}"), paths.get("datawayAdmin"));
                    assertEquals(Set.of("/tools/console", "/tools/console/{*path}"), paths.get("datawayUi"));
                }
            });
        }
    }

    @Test
    void hostMvcInterceptorSuppliesIdentityAndOwnsResourceAccess() {
        context.withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, DatawayAutoConfigurationTest::hostMvc).withBean(HostExceptionHandler.class, HostExceptionHandler::new).withBean(DatawayConfig.class, () -> {
            DatawayConfig builder = new DatawayConfig();
            builder.dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
                assertEquals("visited", request.getAttribute("host.mvc"));
                Object identity = request.getAttribute("host.user");
                return identity instanceof UserIdentity user ? user : UserIdentity.anonymous();
            }).authorizationCheck((identity, operation) -> !"denied-user".equals(identity.id()));
            return builder;
        }).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            publish(c.getBean(Dataway.class), "return 'ok';");
            for (String path : new String[] { "/api/hello", "/dataway/api/api-list", "/dataway/assets/app.js" }) {
                assertEquals(401, mvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getStatus());
                var result = mvc.perform(MockMvcRequestBuilders.get(path).header("X-Test-User", "user")).andReturn();
                assertInstanceOf(HandlerMethod.class, result.getHandler());
                assertEquals(200, result.getResponse().getStatus());
            }
            for (String path : new String[] { "/api/hello", "/dataway/api/api-list" }) {
                var denied = mvc.perform(MockMvcRequestBuilders.get(path).header("X-Test-User", "denied-user")).andReturn();
                assertEquals(401, denied.getResponse().getStatus());
                assertEquals("spring", denied.getResponse().getHeader("X-Host-Error"));
                assertInstanceOf(DatawayException.class, denied.getResolvedException());
            }
            assertEquals(200, mvc.perform(MockMvcRequestBuilders.get("/dataway/assets/app.js").header("X-Test-User", "denied-user")).andReturn().getResponse().getStatus());
            assertEquals(401, mvc.perform(MockMvcRequestBuilders.get("/dataway/host-route")).andReturn().getResponse().getStatus());
            assertEquals(404, mvc.perform(MockMvcRequestBuilders.get("/dataway/host-route").header("X-Test-User", "user")).andReturn().getResponse().getStatus());
        });
    }

    @RestControllerAdvice
    static class HostExceptionHandler {
        @ExceptionHandler({ DatawayException.class, IOException.class })
        ResponseEntity<Map<String, String>> handle(Exception failure) {
            int status = failure instanceof DatawayException error ? error.status() : 500;
            return ResponseEntity.status(status).header("X-Host-Error", "spring").body(Map.of("message", "Handled by the host"));
        }
    }

    @Test
    void checkedExceptionsReachHostControllerAdviceUnchanged() {
        IOException failure = new IOException("host decides the response");
        context.withBean(HostExceptionHandler.class, HostExceptionHandler::new).withBean(Dataway.class, () -> new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway()).withPropertyValues("dataway.api-enabled=true").run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).addFilter((request, response, chain) -> {
                chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
                    @Override
                    public ServletInputStream getInputStream() throws IOException {
                        throw failure;
                    }
                }, response);
            }).build();
            var result = mvc.perform(MockMvcRequestBuilders.get("/api/failure")).andReturn();
            assertSame(failure, result.getResolvedException());
            assertEquals(500, result.getResponse().getStatus());
            assertEquals("spring", result.getResponse().getHeader("X-Host-Error"));
        });
    }

    @Test
    void bodyAccessIsLazyAndUsesTheRequestWrapperSelectedByHost() throws Exception {
        var reads = new AtomicInteger();
        var input = new MockHttpServletRequest("POST", "/api/example");
        input.setContentType("application/json");
        var wrapped = new HttpServletRequestWrapper(input) {
            @Override
            public ServletInputStream getInputStream() {
                reads.incrementAndGet();
                return new DelegatingServletInputStream(new ByteArrayInputStream("{\"name\":\"host-cached\"}".getBytes(StandardCharsets.UTF_8)));
            }
        };
        var request = new SpringWebRequest(wrapped);
        assertEquals(0, reads.get());
        assertTrue(new String(request.getBody().readAllBytes(), StandardCharsets.UTF_8).contains("host-cached"));
        assertEquals(1, reads.get());
    }

    @Test
    void replayableHostRequestSurvivesMvcBodyInspectionBeforeDataway() {
        context.withBean("metadataStorage", DatawayConfig.class, () -> new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            UserIdentity identity = (UserIdentity) request.getAttribute("host.user");
            assertEquals("host-user", identity.id());
            return identity;
        })).withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, () -> new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        assertInstanceOf(HandlerMethod.class, handler);
                        assertTrue(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("host-body"));
                        request.setAttribute("host.user", UserIdentity.authenticated("host-user"));
                        return true;
                    }
                });
            }
        }).withPropertyValues("dataway.api-enabled=true").run(c -> {
            var service = c.getBean(Dataway.class);
            ApiDefinition echoApi = new ApiDefinition();
            echoApi.setId("echo");
            echoApi.setMethod("POST");
            echoApi.setPath("/echo");
            echoApi.setType(ApiScriptType.DATA_QL);
            echoApi.setScript("return ${name};");
            echoApi.setDescription("");
            service.getAdminService().save(echoApi, 0);
            service.getAdminService().publish("echo", 1);
            Filter replay = (input, output, chain) -> {
                byte[] cached = input.getInputStream().readAllBytes();
                var wrapped = new HttpServletRequestWrapper((HttpServletRequest) input) {
                    @Override
                    public ServletInputStream getInputStream() {
                        return new DelegatingServletInputStream(new ByteArrayInputStream(cached));
                    }
                };
                chain.doFilter(wrapped, output);
            };
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).addFilters(replay).build();
            var result = mvc.perform(post("/api/echo").contentType("application/json").content("{\"name\":\"host-body\"}")).andReturn().getResponse();
            assertEquals(200, result.getStatus());
            assertEquals("host-body", JsonUtils.readTree(result.getContentAsString()).get("value").asText());
        });
    }

    @Test
    void responseTemplatesAreValidatedAcrossRequests() throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).createDataway();
        ApiDefinition api = new ApiDefinition();
        api.setType(ApiScriptType.DATA_QL);
        api.setScript("""
                if (${fail}) {
                    throw 409, 'conflict';
                }
                return ${value};
                """);
        // Keep an explicit JSON null in the HTTP payload instead of omitting the option.
        for (Object invalid : Arrays.asList(JsonUtils.readTree("null"), false, 1, "", "{broken", "null", "[]", "1", "true", "\"text\"")) {
            var error = assertThrows(DatawayException.class, () -> this.preview(dataway, api.getScript(), Map.of(), Collections.singletonMap("responseFormat", invalid)));
            assertEquals("responseFormat must be a JSON object string", error.getMessage());
        }

        String template = """
                {"ok":"@resultStatus","data":"@resultData","code":"@resultCode",
                 "literal":{"items":[{"label":"unchanged"}]},"empty":null,"text":"@unknown"}
                """;
        Map<?, ?> first = (Map<?, ?>) this.preview(dataway, api.getScript(), Map.of("fail", false, "value", "one"), Map.of("responseFormat", template));
        assertEquals(Boolean.TRUE, first.get("ok"));
        assertEquals("one", first.get("data"));
        assertFalse(first.containsKey("empty"));
        assertEquals("@unknown", first.get("text"));
        Map<?, ?> literal = (Map<?, ?>) first.get("literal");
        List<?> items = (List<?>) literal.get("items");
        ((Map<?, ?>) items.getFirst()).clear();
        items.clear();
        literal.clear();

        Map<?, ?> failed = (Map<?, ?>) this.preview(dataway, api.getScript(), Map.of("fail", true), Map.of("responseFormat", template));
        assertEquals(Boolean.FALSE, failed.get("ok"));
        assertEquals(409, ((Number) failed.get("code")).intValue());
        assertEquals("conflict", failed.get("data"));
        assertEquals(Map.of("items", List.of(Map.of("label", "unchanged"))), failed.get("literal"));

        Map<?, ?> repeated = (Map<?, ?>) this.preview(dataway, api.getScript(), Map.of("fail", false, "value", "two"), Map.of("responseFormat", template));
        assertEquals(Boolean.TRUE, repeated.get("ok"));
        assertEquals("two", repeated.get("data"));
        assertEquals(failed.get("literal"), repeated.get("literal"));
        Map<?, ?> defaults = (Map<?, ?>) this.preview(dataway, api.getScript(), Map.of("fail", false, "value", "default"), Map.of());
        assertEquals(Boolean.TRUE, defaults.get("success"));
        assertEquals("default", defaults.get("value"));
    }

    private Object preview(Dataway dataway, String script, Map<String, ?> parameters, Map<String, ?> options) throws Exception {
        var input = new MockHttpServletRequest("POST", "/dataway/api/perform");
        Map<String, Object> command = Map.of("id", "-1", "select", "POST", "apiPath", "/preview", "codeType", "DataQL", "codeValue", script, "requestBody", parameters, "optionInfo", options);
        input.setContent(JsonUtils.writeValueAsString(command).getBytes(StandardCharsets.UTF_8));
        var request = new SpringWebRequest(input);
        request.setMethod("POST");
        request.setPath("/dataway/api/perform");
        request.setPathInfo("/perform");
        request.setHeaders(Map.of("Content-Type", "application/json"));
        var output = new MockHttpServletResponse();
        dataway.getAdminHandler().handle(request, new SpringWebResponse(output));
        assertEquals(200, output.getStatus());
        return JsonUtils.readValue(output.getContentAsString(), Object.class);
    }

    @Test
    void apiExecutionAndAdministrationUseSeparateInterceptorChains() {
        List<String> calls = new ArrayList<>();
        UserIdentity identity = UserIdentity.authenticated("query-user");
        DatawayConfig config = new DatawayConfig().resultStructure(false).apiInterceptor((invocation, next) -> {
            assertEquals(Operation.INVOKE, invocation.getOperation());
            assertSame(identity, invocation.getIdentity());
            assertEquals("one", invocation.getDefinition().getId());
            calls.add("api-before");
            Object result = next.proceed(invocation);
            calls.add("api-after");
            return result;
        }).apiInterceptor((invocation, next) -> {
            calls.add("script");
            if (invocation.getParameters().containsKey("shortcut")) {
                byte[] body = "intercepted".getBytes(StandardCharsets.UTF_8);
                ResultInfo result = new ResultInfo();
                result.setData(body);
                result.setJson(false);
                result.getHeaders().put("Content-Type", "text/plain");
                result.getHeaders().put("Content-Length", String.valueOf(body.length));
                return result;
            }
            return next.proceed(invocation);
        });
        Dataway dataway = new Dataway(config.dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> identity).adminInterceptor((action, next) -> {
            assertEquals(Operation.LIST, action.getOperation());
            calls.add("admin-before");
            Object result = next.proceed();
            calls.add("admin-after");
            return result;
        }).adminInterceptor((action, next) -> {
            assertSame(identity, action.getIdentity());
            calls.add("admin-second-before");
            Object result = next.proceed();
            calls.add("admin-second-after");
            return result;
        }));
        this.context.withBean(Dataway.class, () -> dataway).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").run(c -> {
            publish(dataway, "return ${value};");
            calls.clear();
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var response = mvc.perform(MockMvcRequestBuilders.get("/api/hello?value=http")).andReturn().getResponse();
            assertEquals(200, response.getStatus());
            assertEquals("http", JsonUtils.readValue(response.getContentAsString(), String.class));
            assertEquals(List.of("api-before", "script", "api-after"), calls);

            calls.clear();
            var shortcut = mvc.perform(MockMvcRequestBuilders.get("/api/hello?shortcut=true")).andReturn().getResponse();
            assertEquals("intercepted", shortcut.getContentAsString());
            assertEquals(List.of("api-before", "script", "api-after"), calls);

            calls.clear();
            var management = mvc.perform(MockMvcRequestBuilders.get("/dataway/api/api-list")).andReturn().getResponse();
            assertEquals(200, management.getStatus());
            assertEquals(List.of("admin-before", "admin-second-before", "admin-second-after", "admin-after"), calls);

            calls.clear();
            assertEquals(1, dataway.getAdminService().list().size());
            assertTrue(calls.isEmpty());
        });
    }

    private static Map<String, Set<String>> mappings(RequestMappingHandlerMapping mapping) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        mapping.getHandlerMethods().forEach((info, method) -> {
            if (method.getBean() instanceof DatawayController) {
                result.put(info.getName(), info.getPatternValues());
            }
        });
        return result;
    }

    private static WebMvcConfigurer hostMvc() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if (!(handler instanceof HandlerMethod method) || !(method.getBean() instanceof DatawayController)) {
                            return true;
                        }
                        String user = request.getHeader("X-Test-User");
                        if (user == null) {
                            response.setStatus(401);
                            return false;
                        }
                        request.setAttribute("host.mvc", "visited");
                        request.setAttribute("host.user", UserIdentity.authenticated(user));
                        return true;
                    }
                });
            }
        };
    }

    @Test
    void actualServletServerMountsThreeCoreEntriesAfterHostAuthentication() throws Exception {
        JdbcDataSource source = TestDatabase.create();
        AtomicInteger uiRequests = new AtomicInteger();
        try (var application = new AnnotationConfigServletWebServerApplicationContext()) {
            application.getEnvironment().getPropertySources().addFirst(configuration("configured/application.yml"));
            application.registerBean(DataSource.class, () -> source);
            application.registerBean(TomcatServletWebServerFactory.class, () -> {
                var factory = new TomcatServletWebServerFactory(0);
                factory.setContextPath("/host");
                factory.setRegisterDefaultServlet(true);
                return factory;
            });
            application.registerBean("hostIdentity", FilterRegistrationBean.class, () -> {
                Filter identity = (input, output, chain) -> {
                    String user = ((HttpServletRequest) input).getHeader("X-Test-User");
                    if (user != null) {
                        input.setAttribute("host.user", UserIdentity.authenticated(user));
                    }
                    chain.doFilter(input, output);
                };
                var registration = new FilterRegistrationBean<>(identity);
                registration.setName("hostIdentity");
                registration.setOrder(-100);
                registration.addUrlPatterns("/*");
                return registration;
            });
            application.registerBean(DatawayConfig.class, () -> {
                DatawayConfig builder = new DatawayConfig();
                builder.dataAccessLayer(new JdbcDataAccessLayer(source, ""));
                AdminInterceptor policy = (action, chain) -> {
                    if (!"native-user".equals(action.getIdentity().id())) {
                        throw new DatawayException(401, "Host filter must establish identity");
                    }
                    return chain.proceed();
                };
                builder.authorizationCheck((identity, operation) -> "native-user".equals(identity.id()));
                builder.adminInterceptor(policy);
                builder.apiInterceptor((invocation, next) -> {
                    if (invocation.getParameters().containsKey("download")) {
                        ResultInfo result = ResultInfoUtils.convertToResultInfo("application/pdf", new ByteArrayInputStream(new byte[] { 0, 1, (byte) 255 }));
                        result.getHeaders().put("Content-Disposition", "attachment; filename=result.pdf");
                        return result;
                    }
                    return next.proceed(invocation);
                });
                builder.identityProvider(request -> {
                    Object attribute = request.getAttribute("host.user");
                    UserIdentity identity = attribute instanceof UserIdentity user ? user : UserIdentity.anonymous();
                    if (request.getPath().equals("/tools/console" + request.getPathInfo())) {
                        assertEquals("native-user", identity.id());
                        uiRequests.incrementAndGet();
                    }
                    return identity;
                });
                return builder;
            });
            application.register(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class);
            application.register(HostExceptionHandler.class);
            application.registerBean(WebMvcConfigurer.class, () -> hostMvc());
            application.refresh();
            Dataway dataway = application.getBean(Dataway.class);

            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getAdminUiHandler());

            publish(dataway, "return 'spring';");
            try (var connection = source.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM interface_info")) {
                assertTrue(rows.next());
                assertEquals(1, rows.getInt(1));
            }
            String base = "http://127.0.0.1:" + application.getWebServer().getPort() + "/host";
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                assertEquals("spring", JsonUtils.readTree(get(client, base + "/open/v2/hello", "native-user").body()).get("value").asText());
                var binary = client.send(HttpRequest.newBuilder(URI.create(base + "/open/v2/hello?download=true")).header("X-Test-User", "native-user").build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, binary.statusCode());
                assertEquals("application/pdf", binary.headers().firstValue("Content-Type").orElseThrow());
                ApiDefinition cookiesApi = new ApiDefinition();
                cookiesApi.setId("cookies");
                cookiesApi.setMethod("GET");
                cookiesApi.setPath("/cookies");
                cookiesApi.setType(ApiScriptType.DATA_QL);
                cookiesApi.setScript("""
                        import 'net.hasor.dataway.function.WebUdfSource' as w;
                        var a = w.setHeader('X-Result', 'first');
                        var b = w.addHeader('X-Result', 'second');
                        var c = w.setCookie('one', '1');
                        var d = w.setCookie('two', '2', {'httpOnly': true});
                        return {'headers': w.headerArray('X-Repeat'), 'cookies': w.cookieArray('id')};
                        """);
                cookiesApi.setDescription("");
                dataway.getAdminService().save(cookiesApi, 0);
                dataway.getAdminService().publish("cookies", 1);
                var cookies = client.send(HttpRequest.newBuilder(URI.create(base + "/open/v2/cookies"))//
                        .header("X-Test-User", "native-user").header("X-Repeat", "one").header("X-Repeat", "two")//
                        .header("Cookie", "id=first; id=second").build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, cookies.statusCode());
                assertEquals(List.of("first", "second"), cookies.headers().allValues("X-Result"));
                assertEquals(List.of("one=1; Path=/", "two=2; Path=/; HttpOnly"), cookies.headers().allValues("Set-Cookie"));
                var cookieBody = JsonUtils.readTree(cookies.body()).get("value");
                assertEquals("one", cookieBody.get("headers").get(0).asText());
                assertEquals("two", cookieBody.get("headers").get(1).asText());
                assertEquals("first", cookieBody.get("cookies").get(0).asText());
                assertEquals("second", cookieBody.get("cookies").get(1).asText());

                assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, binary.body());
                assertEquals(401, get(client, base + "/ops/manage/api-list", null).statusCode());
                assertEquals(200, get(client, base + "/ops/manage/api-list", "native-user").statusCode());
                var page = get(client, base + "/tools/console/", "native-user");
                assertEquals(200, page.statusCode());
                assertEquals(200, get(client, base + "/tools/console/assets/app.js", "native-user").statusCode());
                var redirect = get(client, base + "/tools/console", "native-user");
                assertEquals(308, redirect.statusCode());
                assertEquals("console/", redirect.headers().firstValue("Location").orElseThrow());
                assertEquals(3, uiRequests.get());
                assertEquals(401, get(client, base + "/tools/console/host-route", null).statusCode());
                assertEquals(3, uiRequests.get(), "Host authentication runs before resource lookup");
                assertEquals(404, get(client, base + "/tools/console/host-route", "native-user").statusCode());
                assertEquals(4, uiRequests.get(), "The UI handler rejects a missing resource after authentication");
                assertEquals(404, get(client, base + "/tools/console-other", null).statusCode());
                assertEquals(404, get(client, base + "/host-route", null).statusCode());
                assertEquals(4, uiRequests.get(), "Paths outside the UI prefix must not reach the handler");
                assertEquals(401, get(client, base + "/tools/console/assets/app.css", null).statusCode());
                assertEquals(4, uiRequests.get(), "Resource denial must happen before the custom handler");

            }
        }
    }

    private static PropertySource<?> configuration(String resource) throws IOException {
        var source = new YamlPropertySourceLoader().load("dataway-test", new ClassPathResource(resource)).getFirst();
        var properties = assertInstanceOf(EnumerablePropertySource.class, source);
        Set<String> expectedKeys = Set.of("dataway.api-enabled", "dataway.api-prefix", "dataway.admin-enabled", "dataway.admin-prefix", "dataway.admin-ui", "dataway.metadata.bean");
        assertEquals(expectedKeys, Set.of(properties.getPropertyNames()));
        return source;
    }

    private static void publish(Dataway service, String script) {
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/hello");
        oneApi.setType(ApiScriptType.DATA_QL);
        oneApi.setScript(script);
        oneApi.setDescription("");
        service.getAdminService().save(oneApi, 0);
        service.getAdminService().publish("one", 1);
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String user) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (user != null) {
            request.header("X-Test-User", user);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

}
