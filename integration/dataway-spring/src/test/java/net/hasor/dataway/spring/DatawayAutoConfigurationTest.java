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
import java.security.Principal;
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
import net.hasor.dataway.model.ResultInfoUtils;
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
        var runner = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class)).withBean(Dataway.class, () -> Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()).build()).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").withBean("hostRoutes", RequestMappingHandlerMapping.class, RequestMappingHandlerMapping::new);
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
            var runner = context.withBean(DatawayConfigurer.class, () -> builder -> builder.configureHost(host -> {
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
    void sameCoreSpiAndSuppliedInstanceRemainSupported() {
        context.withBean(DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer())).run(c -> {
            Dataway dataway = c.getBean(Dataway.class);
            publish(dataway.getService(), "return 'configured';");
            assertEquals("configured", ((Map<?, ?>) dataway.getService().invokeApi("/hello", Map.of())).get("value"));
            assertTrue(c.getBeansOfType(DatawayService.class).isEmpty());
            assertTrue(c.getBeansOfType(WebHandler.class).isEmpty());
            assertTrue(c.getBeansOfType(FilterRegistrationBean.class).isEmpty());
        });
        Dataway supplied = Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()).build();
        context.withBean(Dataway.class, () -> supplied).withBean(DatawayConfigurer.class, () -> builder -> fail("Already assembled")).withPropertyValues("dataway.api-enabled=true").run(c -> {
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
            context.withBean("metadataStorage", DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer())).withBean(DataSource.class, TestDatabase::create).withInitializer(c -> c.getEnvironment().getPropertySources().addLast(configured)).withPropertyValues("dataway.api-enabled=" + api, "dataway.admin-enabled=" + admin).run(c -> {
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
        context.withBean("metadataStorage", DatawayConfigurer.class, () -> builder -> {
            builder.dataAccessLayer(TestDatabase.dataAccessLayer());
        }).withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, DatawayAutoConfigurationTest::hostMvc).withBean(HostExceptionHandler.class, HostExceptionHandler::new).withBean(DatawayConfigurer.class, () -> builder -> {
            builder.dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
                assertEquals("visited", request.getAttribute("host.mvc"));
                return request.getIdentity();
            }).authorizationCheck((identity, operation) -> !"denied-user".equals(identity.getId()));
        }).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            publish(c.getBean(Dataway.class).getService(), "return 'ok';");
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
        context.withBean(HostExceptionHandler.class, HostExceptionHandler::new).withBean(Dataway.class, () -> Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()).apiHandler(core -> new FailingWebHandler(core, failure)).build()).withPropertyValues("dataway.api-enabled=true").run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
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
        context.withBean("metadataStorage", DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer())).withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, () -> new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        assertInstanceOf(HandlerMethod.class, handler);
                        assertTrue(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("host-body"));
                        request.setAttribute(RequestAttribute.IDENTITY.getKey(), UserIdentity.authenticated("host-user"));
                        return true;
                    }
                });
            }
        }).withPropertyValues("dataway.api-enabled=true").run(c -> {
            var service = c.getBean(Dataway.class).getService();
            ApiDefinition echoApi = new ApiDefinition();
            echoApi.setId("echo");
            echoApi.setMethod("POST");
            echoApi.setPath("/echo");
            echoApi.setType(ApiScriptType.DATAQL);
            echoApi.setScript("return ${name};");
            echoApi.setDescription("");
            service.getBeanContainer().getAdminService().save(echoApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            service.getBeanContainer().getAdminService().publish("echo", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
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
    void responseTemplatesAreValidatedAndReusedAcrossExecutions() {
        this.context.withBean(Dataway.class, () -> Dataway.builder().dataAccessLayer(TestDatabase.dataAccessLayer()).build()).run(c -> {
            var engine = c.getBean(Dataway.class).getBeanContainer().getEngine();
            ApiDefinition api = new ApiDefinition();
            api.setType(ApiScriptType.DATAQL);
            api.setScript("""
                    if (${fail}) {
                        throw 409, 'conflict';
                    }
                    return ${value};
                    """);
            for (Object invalid : Arrays.asList(null, false, 1, "", "{broken", "null", "[]", "1", "true", "\"text\"")) {
                var error = assertThrows(DatawayException.class, () -> engine.newQuery(api, List.of("fail", "value"), Collections.singletonMap("responseFormat", invalid)));
                assertEquals("responseFormat must be a JSON object string", error.getMessage());
            }

            String template = """
                    {"ok":"@resultStatus","data":"@resultData","code":"@resultCode",
                     "literal":{"items":[{"label":"unchanged"}]},"empty":null,"text":"@unknown"}
                    """;
            var query = engine.newQuery(api, List.of("fail", "value"), Map.of("responseFormat", template));
            Map<?, ?> first = (Map<?, ?>) query.execute(Operation.INVOKE, UserIdentity.anonymous(), Map.of("fail", false, "value", "one"), null, null);
            assertEquals(Boolean.TRUE, first.get("ok"));
            assertEquals("one", first.get("data"));
            assertTrue(first.containsKey("empty"));
            assertNull(first.get("empty"));
            assertEquals("@unknown", first.get("text"));
            Map<?, ?> literal = (Map<?, ?>) first.get("literal");
            List<?> items = (List<?>) literal.get("items");
            ((Map<?, ?>) items.getFirst()).clear();
            items.clear();
            literal.clear();

            Map<?, ?> failed = (Map<?, ?>) query.execute(Operation.INVOKE, UserIdentity.anonymous(), Map.of("fail", true), null, null);
            assertEquals(Boolean.FALSE, failed.get("ok"));
            assertEquals(409, ((Number) failed.get("code")).intValue());
            assertEquals("conflict", failed.get("data"));
            assertEquals(Map.of("items", List.of(Map.of("label", "unchanged"))), failed.get("literal"));

            Map<?, ?> repeated = (Map<?, ?>) query.execute(Operation.INVOKE, UserIdentity.anonymous(), Map.of("fail", false, "value", "two"), null, null);
            assertEquals(Boolean.TRUE, repeated.get("ok"));
            assertEquals("two", repeated.get("data"));
            assertEquals(failed.get("literal"), repeated.get("literal"));
            var defaultQuery = engine.newQuery(api, List.of("fail", "value"), null);
            Map<?, ?> defaults = (Map<?, ?>) defaultQuery.execute(Operation.INVOKE, UserIdentity.anonymous(), Map.of("fail", false, "value", "default"), null, null);
            assertEquals(Boolean.TRUE, defaults.get("success"));
            assertEquals("default", defaults.get("value"));
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
                        request.setAttribute(RequestAttribute.IDENTITY.getKey(), UserIdentity.authenticated(user));
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
                    var authenticated = new HttpServletRequestWrapper((HttpServletRequest) input) {
                        @Override
                        public Principal getUserPrincipal() {
                            String user = getHeader("X-Test-User");
                            return user == null ? null : () -> user;
                        }
                    };
                    chain.doFilter(authenticated, output);
                };
                var registration = new FilterRegistrationBean<>(identity);
                registration.setName("hostIdentity");
                registration.setOrder(-100);
                registration.addUrlPatterns("/*");
                return registration;
            });
            application.registerBean(DatawayConfigurer.class, () -> builder -> {
                builder.dataAccessLayer(new JdbcDataAccessLayer(source, ""));
                AdminInterceptor policy = (action, chain) -> {
                    if (!"native-user".equals(action.getIdentity().getId())) {
                        throw new DatawayException(401, "Host filter must establish identity");
                    }
                    return chain.proceed();
                };
                builder.authorizationCheck((identity, operation) -> "native-user".equals(identity.getId()));
                builder.actionInterceptor(policy);
                builder.configureService(service -> service.interceptor((invocation, next) -> {
                    if (invocation.getParameters().containsKey("download")) {
                        ResultInfo result = ResultInfoUtils.ofStream("application/pdf", new ByteArrayInputStream(new byte[] { 0, 1, (byte) 255 }));
                        result.getHeaders().put("Content-Disposition", "attachment; filename=result.pdf");
                        return result;
                    }
                    return next.proceed(invocation);
                }));
                builder.identityProvider(request -> {
                    if (request.getPath().equals("/tools/console" + request.getPathInfo())) {
                        assertEquals("native-user", request.getIdentity().getId());
                        uiRequests.incrementAndGet();
                    }
                    return request.getIdentity();
                });
            });
            application.register(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class);
            application.register(HostExceptionHandler.class);
            application.registerBean(WebMvcConfigurer.class, () -> hostMvc());
            application.refresh();
            Dataway dataway = application.getBean(Dataway.class);

            assertNotNull(dataway.getApiHandler());
            assertNotNull(dataway.getAdminHandler());
            assertNotNull(dataway.getUiHandler());

            publish(dataway.getService(), "return 'spring';");
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
                dataway.getAdminService().save(cookiesApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
                dataway.getAdminService().publish("cookies", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
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

    private static void publish(DatawayService service, String script) {
        ApiDefinition oneApi = new ApiDefinition();
        oneApi.setId("one");
        oneApi.setMethod("GET");
        oneApi.setPath("/hello");
        oneApi.setType(ApiScriptType.DATAQL);
        oneApi.setScript(script);
        oneApi.setDescription("");
        service.getBeanContainer().getAdminService().save(oneApi, 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        service.getBeanContainer().getAdminService().publish("one", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String user) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (user != null) {
            request.header("X-Test-User", user);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

}
