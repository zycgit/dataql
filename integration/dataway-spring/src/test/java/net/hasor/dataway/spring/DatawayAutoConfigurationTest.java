/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayConfigurer;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.exception.DatawayException;
import net.hasor.dataway.execution.CallContext;
import net.hasor.dataway.function.FxRuntime;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ScriptType;
import net.hasor.dataway.repository.ApiRepository;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.web.WebEntry;
import net.hasor.dataway.web.WebHandler;
import net.hasor.dataway.web.WebOptions;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatawayAutoConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class));

    @Test
    void coreControlsOptOutAndRepositoryRequirements() {
        context.withPropertyValues("dataway.embedded.enabled=false").run(c -> {
            assertNull(c.getStartupFailure());
            Dataway dataway = c.getBean(Dataway.class);
            assertFalse(dataway.isEnabled());
            assertNull(dataway.getService());
            assertTrue(mount(c.getBean(DatawayServletInitializer.class)).isEmpty());
        });
        context.run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void springOnlyExposesCoreAndServletIntegrationAndCollectsTheCoreSpi() {
        DatawayConfigurer extension = builder -> {
            builder.inMemory(true);
            builder.configureRuntime(runtime -> runtime.function("answer", (hints, args) -> "managed"));
        };
        context.withBean(DatawayConfigurer.class, () -> extension).run(c -> {
            assertNull(c.getStartupFailure());
            Dataway dataway = c.getBean(Dataway.class);
            publish(dataway.getService(), "return answer();");
            assertEquals("managed", dataway.getService().invokeApi("/hello", Map.of()));
            assertTrue(c.getBeansOfType(FxRuntime.class).isEmpty());
            assertTrue(c.getBeansOfType(ApiRepository.class).isEmpty());
            assertTrue(c.getBeansOfType(DatawayService.class).isEmpty());
            assertTrue(c.getBeansOfType(WebHandler.class).isEmpty());
            assertTrue(c.getBeansOfType(WebOptions.class).isEmpty());
            assertTrue(c.getBeansOfType(FilterRegistrationBean.class).isEmpty());
            assertEquals(100, c.getBean(DatawayServletInitializer.class).getOrder());
            assertThrows(ClassNotFoundException.class, () -> Class.forName("net.hasor.core.AppContext"));
        });
    }

    @Test
    void suppliedCoreInstanceIsMountedWithoutReassemblyOrConfigurationRebinding() {
        WebOptions options = new WebOptions();
        options.setApiPrefix("/assembled");
        Dataway supplied = Dataway.builder().inMemory(true).webOptions(options).build();
        context.withBean(Dataway.class, () -> supplied)
                .withBean(DatawayConfigurer.class, () -> builder -> fail("Core was already assembled"))
                .withPropertyValues("dataway.embedded.api-prefix=/unused").run(c -> {
                    assertNull(c.getStartupFailure());
                    assertSame(supplied, c.getBean(Dataway.class));
                    assertEquals("/assembled", c.getBean(Dataway.class).getHandlers().get(WebEntry.API).pathPrefix());
                    assertEquals(1, mount(c.getBean(DatawayServletInitializer.class)).size());
                });
    }

    @Test
    void serviceSuppliedThroughCoreSpiDoesNotRequireAnotherRepositoryOrDatasource() {
        DatawayService service = Dataway.builder().inMemory(true).build().getService();
        context.withPropertyValues("dataway.embedded.initialize-schema=true")
                .withBean(DatawayConfigurer.class, () -> builder -> builder.service(service)
                        .configureRuntime(runtime -> fail("Unexpected runtime assembly")))
                .run(c -> {
                    assertNull(c.getStartupFailure());
                    assertSame(service, c.getBean(Dataway.class).getService());
                });
    }

    @Test
    void everyEnabledCombinationIsMountedAsSeparateServletFilters() {
        for (int mask = 0; mask < 8; mask++) {
            boolean api = (mask & 1) != 0;
            boolean admin = (mask & 2) != 0;
            boolean ui = (mask & 4) != 0;
            context.withPropertyValues("dataway.embedded.in-memory=true", "dataway.embedded.api-enabled=" + api,
                    "dataway.embedded.admin-enabled=" + admin, "dataway.embedded.ui-enabled=" + ui,
                    "dataway.embedded.admin-token=spring-test-token",
                    "dataway.embedded.api-prefix=/open/v2", "dataway.embedded.admin-prefix=/ops/manage",
                    "dataway.embedded.ui-prefix=/tools/console").run(c -> {
                assertNull(c.getStartupFailure());
                Dataway dataway = c.getBean(Dataway.class);
                Map<String, Registration> filters = mount(c.getBean(DatawayServletInitializer.class));
                assertEquals(dataway.getHandlers().size(), filters.size());
                assertEquals(api, filters.containsKey(WebEntry.API.getRegistrationName()));
                assertEquals(admin, filters.containsKey(WebEntry.ADMIN.getRegistrationName()));
                assertEquals(ui, filters.containsKey(WebEntry.UI.getRegistrationName()));
                for (var entry : dataway.getHandlers().entrySet()) {
                    ArgumentCaptor<String[]> paths = ArgumentCaptor.forClass(String[].class);
                    Registration registration = filters.get(entry.getKey().getRegistrationName());
                    verify(registration.dynamic()).addMappingForUrlPatterns(any(), eq(false), paths.capture());
                    String prefix = entry.getValue().pathPrefix();
                    assertArrayEquals(new String[] { prefix, prefix + "/*" }, paths.getValue());
                }
                if (ui) {
                    var page = new MockHttpServletRequest("GET", "/host/tools/console/");
                    page.setContextPath("/host");
                    var response = new MockHttpServletResponse();
                    filters.get(WebEntry.UI.getRegistrationName()).filter().doFilter(page, response, new MockFilterChain());
                    assertTrue(response.getContentAsString().contains("content=\"../../ops/manage/\""));
                    assertTrue(response.getContentAsString().contains("content=\"../../open/v2/\""));
                }
                if (ui && !admin) {
                    var request = new MockHttpServletRequest("GET", "/host/tools/console/unrelated");
                    request.setContextPath("/host");
                    var chain = new MockFilterChain();
                    filters.get(WebEntry.UI.getRegistrationName()).filter()
                            .doFilter(request, new MockHttpServletResponse(), chain);
                    assertSame(request, chain.getRequest());
                }
            });
        }
    }

    @Test
    void uiOnlyDoesNotNeedDatabaseOrExecutionServices() {
        context.withPropertyValues("dataway.embedded.api-enabled=false", "dataway.embedded.ui-enabled=true")
                .withBean(DatawayConfigurer.class, () -> builder -> builder
                        .dataSource(() -> { throw new AssertionError("UI must not resolve a datasource"); }))
                .run(c -> {
                    assertNull(c.getStartupFailure());
                    assertNull(c.getBean(Dataway.class).getService());
                    assertNotNull(c.getBean(Dataway.class).getHandlers().get(WebEntry.UI));
                });
    }

    @Test
    void webOptionsSubclassIsInjectedThroughTheSameCoreSpi() {
        WebOptions options = new WebOptions() {
            @Override
            public String getApiPrefix() {
                return "/tenant-one" + super.getApiPrefix();
            }
        };
        context.withPropertyValues("dataway.embedded.in-memory=true", "dataway.embedded.api-prefix=/open")
                .withBean(DatawayConfigurer.class, () -> builder -> builder.webOptions(options)).run(c -> {
                    assertNull(c.getStartupFailure());
                    Dataway dataway = c.getBean(Dataway.class);
                    assertSame(options, dataway.getWebOptions());
                    assertEquals("/tenant-one/open", dataway.getHandlers().get(WebEntry.API).pathPrefix());
                });
        context.withPropertyValues("dataway.embedded.in-memory=true", "dataway.embedded.api-prefix=/bad?path")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void adminAuthorizationComesFromTheCoreSpiAndReceivesServletIdentity() {
        context.withPropertyValues("dataway.embedded.in-memory=true", "dataway.embedded.admin-enabled=true")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("dataway.embedded.in-memory=true", "dataway.embedded.admin-enabled=true")
                .withBean(DatawayConfigurer.class, () -> builder -> builder.getWebOptions()
                        .setAdminAuthorizer(request -> "admin".equals(request.principal())))
                .run(c -> {
                    assertNull(c.getStartupFailure());
                    var filters = mount(c.getBean(DatawayServletInitializer.class));
                    var request = new MockHttpServletRequest("GET", "/dataway/api/apis");
                    request.setUserPrincipal(() -> "admin");
                    var response = new MockHttpServletResponse();
                    filters.get(WebEntry.ADMIN.getRegistrationName()).filter()
                            .doFilter(request, response, new MockFilterChain());
                    assertEquals(200, response.getStatus());
                });
    }

    @Test
    void actualServletServerMountsThreeCoreEntriesAfterHostAuthentication() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:spring_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (var application = new AnnotationConfigServletWebServerApplicationContext()) {
            application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("dataway-test", Map.of(
                    "dataway.embedded.initialize-schema", "true",
                    "dataway.embedded.admin-enabled", "true",
                    "dataway.embedded.ui-enabled", "true",
                    "dataway.embedded.admin-token", "native-token",
                    "dataway.embedded.api-prefix", "/open/v2",
                    "dataway.embedded.admin-prefix", "/ops/manage",
                    "dataway.embedded.ui-prefix", "/tools/console")));
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
                            return () -> "native-user";
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
            application.registerBean(DatawayConfigurer.class, () -> builder -> builder.configureService(service -> {
                service.authorization((operation, api, call) -> {
                    if (operation == Operation.INVOKE && !"native-user".equals(call.principal())) {
                        throw new DatawayException(403, "Host filter must run first");
                    }
                });
            }));
            application.register(DatawayAutoConfiguration.class);
            application.refresh();
            publish(application.getBean(Dataway.class).getService(), "return 'spring';");
            String base = "http://127.0.0.1:" + application.getWebServer().getPort() + "/host";
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                assertEquals("\"spring\"", get(client, base + "/open/v2/hello", null).body());
                assertEquals(401, get(client, base + "/ops/manage/apis", null).statusCode());
                assertEquals(200, get(client, base + "/ops/manage/apis", "native-token").statusCode());
                assertEquals(200, get(client, base + "/tools/console/", null).statusCode());
                assertEquals(200, get(client, base + "/tools/console/assets/app.js", null).statusCode());
                assertEquals(404, get(client, base + "/host-route", null).statusCode());
            }
        }
    }

    private static Map<String, Registration> mount(DatawayServletInitializer initializer) throws Exception {
        ServletContext servletContext = mock(ServletContext.class);
        Map<String, Registration> entries = new LinkedHashMap<>();
        when(servletContext.addFilter(anyString(), any(Filter.class))).thenAnswer(call -> {
            String name = call.getArgument(0);
            DatawayFilter filter = call.getArgument(1);
            FilterRegistration.Dynamic dynamic = mock(FilterRegistration.Dynamic.class);
            assertNull(entries.put(name, new Registration(filter, dynamic)));
            return dynamic;
        });
        initializer.onStartup(servletContext);
        return entries;
    }

    private static void publish(DatawayService service, String script) {
        service.save(new ApiDefinition("one", "GET", "/hello", ScriptType.DATAQL, script, ""), 0, CallContext.LOCAL);
        service.publish("one", 1, CallContext.LOCAL);
    }

    private static HttpResponse<String> get(HttpClient client, String uri, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(10));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private record Registration(DatawayFilter filter, FilterRegistration.Dynamic dynamic) {
    }
}
