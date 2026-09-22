/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayConfigurer;
import net.hasor.dataway.spi.DatawayException;
import net.hasor.dataway.spi.DatawayInterceptor;
import net.hasor.dataway.web.DatawayUiHandler;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.dataway.web.WebHandler;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
    void unavailableProviderFailsWhenAnEntryIsEnabled() {
        context.withPropertyValues("dataway.admin-enabled=true", "dataway.metadata.type=missing-provider").run(c -> {
            Throwable failure = c.getStartupFailure();
            assertNotNull(failure);
            while (failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertTrue(failure.getMessage().contains("missing-provider"));
        });
    }

    @Test
    void disabledDefaultsDoNotAssembleCoreOrRegisterMappings() throws Exception {
        for (boolean fromFile : new boolean[] { false, true }) {
            var runner = context.withBean(DatawayConfigurer.class, () -> builder -> builder.dataSource(() -> {
                throw new AssertionError("Disabled entries must not acquire storage");
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
        context.withBean(DatawayConfigurer.class, () -> builder -> builder.dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer())).run(c -> {
            Dataway dataway = c.getBean(Dataway.class);
            publish(dataway.getService(), "return 'configured';");
            assertEquals("configured", dataway.getService().invokeApi("/hello", Map.of()));
            assertTrue(c.getBeansOfType(DatawayService.class).isEmpty());
            assertTrue(c.getBeansOfType(WebHandler.class).isEmpty());
            assertTrue(c.getBeansOfType(FilterRegistrationBean.class).isEmpty());
        });
        Dataway supplied = Dataway.builder().dataSource(TestDatabase.create()).dataAccessLayer(TestDatabase.dataAccessLayer()).build();
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
                    assertEquals(Set.of("/tools/console", "/tools/console/", "/tools/console/assets/app.js", "/tools/console/assets/app.css", "/tools/console/config.json"), paths.get("datawayUi"));
                }
            });
        }
    }

    @Test
    void hostMvcInterceptorSuppliesIdentityAndCanDenyEveryEntry() {
        context.withBean("metadataStorage", DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer())).withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, DatawayAutoConfigurationTest::hostMvc).withBean(DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer()).identityProvider(request -> {
            assertEquals("visited", request.getAttribute("host.mvc"));
            return request.getIdentity();
        })).withPropertyValues("dataway.api-enabled=true", "dataway.admin-enabled=true").run(c -> {
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            publish(c.getBean(Dataway.class).getService(), "return 'ok';");
            for (String path : new String[] { "/api/hello", "/dataway/api/apis", "/dataway/assets/app.js" }) {
                assertEquals(401, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)).andReturn().getResponse().getStatus());
                var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("X-Test-User", "user")).andReturn();
                assertInstanceOf(HandlerMethod.class, result.getHandler());
                assertEquals(200, result.getResponse().getStatus());
            }
            assertEquals(404, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/dataway/host-route")).andReturn().getResponse().getStatus());
        });
    }

    @Test
    void bodyAccessIsLazyAndUsesTheRequestWrapperSelectedByHost() throws Exception {
        var reads = new AtomicInteger();
        var input = new MockHttpServletRequest("POST", "/api/example");
        input.setContentType("application/json");
        var wrapped = new HttpServletRequestWrapper(input) {
            @Override
            public jakarta.servlet.ServletInputStream getInputStream() {
                reads.incrementAndGet();
                return new org.springframework.mock.web.DelegatingServletInputStream(new java.io.ByteArrayInputStream("{\"name\":\"host-cached\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
        };
        var request = new SpringWebRequest(wrapped);
        assertEquals(0, reads.get());
        assertTrue(new String(request.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("host-cached"));
        assertEquals(1, reads.get());
    }

    @Test
    void replayableHostRequestSurvivesMvcBodyInspectionBeforeDataway() {
        context.withBean("metadataStorage", DatawayConfigurer.class, () -> builder -> builder.dataAccessLayer(TestDatabase.dataAccessLayer())).withBean(DataSource.class, TestDatabase::create).withBean(WebMvcConfigurer.class, () -> new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
                        assertInstanceOf(HandlerMethod.class, handler);
                        assertTrue(new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("host-body"));
                        request.setAttribute(RequestAttribute.IDENTITY.getKey(), UserIdentity.authenticated("host-user"));
                        return true;
                    }
                });
            }
        }).withPropertyValues("dataway.api-enabled=true").run(c -> {
            var service = c.getBean(Dataway.class).getService();
            service.save(new ApiDefinition("echo", "POST", "/echo", ScriptType.DATAQL, "return ${name};", ""), 0, CallContext.LOCAL);
            service.publish("echo", 1, CallContext.LOCAL);
            Filter replay = (input, output, chain) -> {
                byte[] cached = input.getInputStream().readAllBytes();
                var wrapped = new HttpServletRequestWrapper((HttpServletRequest) input) {
                    @Override
                    public jakarta.servlet.ServletInputStream getInputStream() {
                        return new org.springframework.mock.web.DelegatingServletInputStream(new java.io.ByteArrayInputStream(cached));
                    }
                };
                chain.doFilter(wrapped, output);
            };
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).addFilters(replay).build();
            var result = mvc.perform(post("/api/echo").contentType("application/json").content("{\"name\":\"host-body\"}")).andReturn().getResponse();
            assertEquals(200, result.getStatus());
            assertEquals("\"host-body\"", result.getContentAsString());
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
                    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
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
                builder.dataAccessLayer(new net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer(source, ""));
                DatawayInterceptor policy = (action, chain) -> {
                    if (action.getCallContext().source().equals("HTTP") && !"native-user".equals(action.getIdentity().getId())) {
                        throw new DatawayException(401, "Host filter must establish identity");
                    }
                    return chain.proceed();
                };
                builder.accessInterceptor(policy).actionInterceptor(policy);
                builder.configureService(service -> service.interceptor((invocation, next) -> {
                    if (invocation.parameters().containsKey("download")) {
                        return net.hasor.dataway.web.SerializationInfo.ofStream("application/pdf", new java.io.ByteArrayInputStream(new byte[] { 0, 1, (byte) 255 })).withHeader("Content-Disposition", "attachment; filename=result.pdf");
                    }
                    return next.proceed(invocation);
                }));
                builder.uiHandler(dataway -> {
                    var ui = new DatawayUiHandler(dataway);
                    return (request, response) -> {
                        uiRequests.incrementAndGet();
                        ui.handle(request, response);
                    };
                });
            });
            application.register(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class);
            application.registerBean(org.springframework.web.servlet.config.annotation.WebMvcConfigurer.class, () -> hostMvc());
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
                assertEquals("\"spring\"", get(client, base + "/open/v2/hello", "native-user").body());
                var binary = client.send(HttpRequest.newBuilder(URI.create(base + "/open/v2/hello?download=true")).header("X-Test-User", "native-user").build(), HttpResponse.BodyHandlers.ofByteArray());
                assertEquals(200, binary.statusCode());
                assertEquals("application/pdf", binary.headers().firstValue("Content-Type").orElseThrow());
                dataway.getService().save(new ApiDefinition("cookies", "GET", "/cookies", ScriptType.DATAQL, """
                        import 'net.hasor.dataway.function.WebUdfSource' as w;
                        var a = w.setHeader('X-Result', 'first');
                        var b = w.addHeader('X-Result', 'second');
                        var c = w.setCookie('one', '1');
                        var d = w.setCookie('two', '2', {'httpOnly': true});
                        return {'headers': w.headerArray('X-Repeat'), 'cookies': w.cookieArray('id')};
                        """, ""), 0, CallContext.LOCAL);
                dataway.getService().publish("cookies", 1, CallContext.LOCAL);
                var cookies = client.send(HttpRequest.newBuilder(URI.create(base + "/open/v2/cookies"))//
                        .header("X-Test-User", "native-user").header("X-Repeat", "one").header("X-Repeat", "two")//
                        .header("Cookie", "id=first; id=second").build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, cookies.statusCode());
                assertEquals(java.util.List.of("first", "second"), cookies.headers().allValues("X-Result"));
                assertEquals(java.util.List.of("one=1; Path=/", "two=2; Path=/; HttpOnly"), cookies.headers().allValues("Set-Cookie"));
                var cookieBody = tools.jackson.databind.json.JsonMapper.builder().build().readTree(cookies.body());
                assertEquals("one", cookieBody.get("headers").get(0).asText());
                assertEquals("two", cookieBody.get("headers").get(1).asText());
                assertEquals("first", cookieBody.get("cookies").get(0).asText());
                assertEquals("second", cookieBody.get("cookies").get(1).asText());

                assertArrayEquals(new byte[] { 0, 1, (byte) 255 }, binary.body());
                assertEquals(401, get(client, base + "/ops/manage/apis", null).statusCode());
                assertEquals(200, get(client, base + "/ops/manage/apis", "native-user").statusCode());
                var page = get(client, base + "/tools/console/", "native-user");
                assertEquals(200, page.statusCode());
                assertEquals(200, get(client, base + "/tools/console/assets/app.js", "native-user").statusCode());
                var redirect = get(client, base + "/tools/console", "native-user");
                assertEquals(308, redirect.statusCode());
                assertEquals("console/", redirect.headers().firstValue("Location").orElseThrow());
                assertEquals(3, uiRequests.get());
                assertEquals(404, get(client, base + "/tools/console/host-route", null).statusCode());
                assertEquals(404, get(client, base + "/tools/console-other", null).statusCode());
                assertEquals(404, get(client, base + "/host-route", null).statusCode());
                assertEquals(3, uiRequests.get(), "The container must not send host paths to the UI handler");
                assertEquals(401, get(client, base + "/tools/console/assets/app.css", null).statusCode());
                assertEquals(3, uiRequests.get(), "Resource denial must happen before the custom handler");

            }
        }
    }

    private static PropertySource<?> configuration(String resource) throws IOException {
        var source = new YamlPropertySourceLoader().load("dataway-test", new ClassPathResource(resource)).getFirst();
        var properties = assertInstanceOf(EnumerablePropertySource.class, source);
        Set<String> expectedKeys = Set.of("dataway.api-enabled", "dataway.api-prefix", "dataway.admin-enabled", "dataway.admin-prefix", "dataway.admin-ui", "dataway.metadata.type", "dataway.metadata.bean", "dataway.metadata.jdbc.executor", "dataway.metadata.jdbc.data-source", "dataway.metadata.jdbc.transaction-manager", "dataway.metadata.jdbc.table-prefix", "dataway.metadata.nacos.config-service", "dataway.metadata.nacos.data-id", "dataway.metadata.nacos.group", "dataway.metadata.nacos.timeout-millis");
        assertEquals(expectedKeys, Set.of(properties.getPropertyNames()));
        return source;
    }

    private static void publish(DatawayService service, String script) {
        service.save(new ApiDefinition("one", "GET", "/hello", ScriptType.DATAQL, script, ""), 0, CallContext.LOCAL);
        service.publish("one", 1, CallContext.LOCAL);
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String user) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (user != null) {
            request.header("X-Test-User", user);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

}
