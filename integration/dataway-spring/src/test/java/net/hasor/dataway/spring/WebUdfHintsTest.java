/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.Cookie;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class WebUdfHintsTest {
    @Test
    void concurrentRequestsKeepHeadersCookiesAndBodiesInTheirOwnQuery() {
        var context = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class));
        context.withBean(Dataway.class, () -> new Dataway(new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()))).withPropertyValues("dataway.api-enabled=true").run(c -> {
            assertNull(c.getStartupFailure());
            Dataway dataway = c.getBean(Dataway.class);
            ApiDefinition api = new ApiDefinition();
            api.setId("web-context");
            api.setMethod("POST");
            api.setPath("/web-context");
            api.setType(ApiScriptType.DATA_QL);
            api.setDescription("");
            api.setScript("""
                    import 'net.hasor.dataway.function.WebUdfSource' as web;
                    import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                    var inspect = () -> {
                        run web.setHeader('X-Result', web.header('x-request'));
                        run web.addHeader('X-Result', 'direct');
                        run web.setHeaderAll({'X-Batch': 'set'});
                        run web.addHeaderAll({'X-Result': ['nested', 'last']});
                        run web.setCookie('echo', web.cookie('SESSION'), {'HTTPONLY': true});
                        run web.removeCookie('old', {'PATH': '/old'});
                        return {
                            'header': web.header('X-REQUEST'),
                            'repeated': web.headerArray('x-repeated'),
                            'headers': web.headerMap(),
                            'headerArrays': web.headerArrayMap(),
                            'cookie': web.cookieArray('session'),
                            'cookies': web.cookieMap(),
                            'cookieArrays': web.cookieArrayMap(),
                            'body': web.jsonBody(),
                            'parameter': ${message},
                            'callbacks': collect.mapValueReplace({'first': 'x-request'},
                                (key, name) -> { return web.header(name); })
                        };
                    };
                    return inspect();
                    """);
            dataway.getAdminService().save(api, 0);
            dataway.getAdminService().publish(api.getId(), 1);
            var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
            var barrier = new CyclicBarrier(4);
            List<Callable<Void>> requests = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String marker = "request-" + i;
                requests.add(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    var response = mvc.perform(post("/api/web-context?queryOnly=value").header("X-Request", marker).header("X-Repeated", "one", "two").cookie(new Cookie("session", marker)).contentType("application/json").content(JsonUtils.writeValueAsString(Map.of("message", marker)))).andReturn().getResponse();
                    assertEquals(200, response.getStatus());
                    assertEquals(List.of(marker, "direct", "nested", "last"), response.getHeaders("X-Result"));
                    assertEquals("set", response.getHeader("X-Batch"));
                    assertEquals(2, response.getHeaders("Set-Cookie").size());
                    assertTrue(response.getHeaders("Set-Cookie").contains("echo=" + marker + "; Path=/; HttpOnly"));
                    assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(value -> value.startsWith("old=; Path=/old;") && value.contains("Max-Age=0")));
                    var result = JsonUtils.readTree(response.getContentAsString()).get("value");
                    assertEquals(marker, result.get("header").asText());
                    assertEquals(List.of("one", "two"), JsonUtils.readValue(JsonUtils.writeValueAsString(result.get("repeated")), List.class));
                    assertEquals(marker, result.get("headers").get("x-request").asText());
                    assertEquals(marker, result.get("headerArrays").get("x-request").get(0).asText());
                    assertEquals(marker, result.get("cookie").get(0).asText());
                    assertEquals(marker, result.get("cookies").get("session").asText());
                    assertEquals(marker, result.get("cookieArrays").get("session").get(0).asText());
                    assertEquals(Map.of("message", marker), JsonUtils.readValue(JsonUtils.writeValueAsString(result.get("body")), Map.class));
                    assertEquals(marker, result.get("parameter").asText());
                    assertEquals(marker, result.get("callbacks").get("first").asText());
                    return null;
                });
            }
            var executor = Executors.newFixedThreadPool(4);
            try {
                for (var result : executor.invokeAll(requests, 30, TimeUnit.SECONDS)) {
                    result.get();
                }
            } finally {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        });
    }

    @Test
    void requestContextDoesNotLeakAfterSuccessOrFailure() throws Exception {
        Dataway dataway = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).resultStructure(false).createDataway();
        ApiDefinition api = new ApiDefinition();
        api.setId("context");
        api.setMethod("POST");
        api.setPath("/context");
        api.setType(ApiScriptType.DATA_QL);
        api.setDescription("");
        api.setScript("""
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                if (${fail}) {
                    throw 500, 'deliberate failure';
                }
                return [web.header('X-Demo'), web.cookie('session'), web.jsonBody()];
                """);
        dataway.getAdminService().save(api, 0);
        dataway.getAdminService().publish(api.getId(), 1);
        var controller = new DatawayController("/api", dataway.getApiHandler());
        Map<String, ?> body = Map.of("fail", false, "name", "body");
        assertEquals(List.of("value", "cookie", body), this.invoke(controller, body, true));
        List<?> empty = (List<?>) this.invoke(controller, Map.of("fail", false), false);
        assertNull(empty.get(0));
        assertNull(empty.get(1));
        assertEquals(Map.of("fail", false), empty.get(2));

        assertEquals("deliberate failure", this.invoke(controller, Map.of("fail", true), true));
        assertEquals(empty, this.invoke(controller, Map.of("fail", false), false));
        assertEquals(List.of("value", "cookie", body), this.invoke(controller, body, true));
    }

    private Object invoke(DatawayController controller, Map<String, ?> body, boolean withMetadata) throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/context");
        request.setContentType("application/json");
        request.setContent(JsonUtils.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
        if (withMetadata) {
            request.addHeader("X-Demo", "value");
            request.addHeader("Cookie", "session=cookie");
        }
        var response = new MockHttpServletResponse();
        controller.handle(request, response);
        assertEquals(200, response.getStatus());
        return JsonUtils.readValue(response.getContentAsString(), Object.class);
    }
}
