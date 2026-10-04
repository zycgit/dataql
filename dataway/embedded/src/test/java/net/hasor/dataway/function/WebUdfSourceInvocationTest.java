/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.script.ApiCallSource;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.service.script.DatawayQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

class WebUdfSourceInvocationTest {
    private final WebUdfSource source = new WebUdfSource();

    @Test
    void registrationExposesOnlyTheDocumentedFunctions() throws Exception {
        Map<String, Udf> functions = this.source.getUdfResource(new HostConfiguration()).get();

        assertEquals(Set.of("header", "headerArray", "headerMap", "headerArrayMap", "setHeader", "addHeader", "setHeaderAll", "addHeaderAll", "cookie", "cookieArray", "cookieMap", "cookieArrayMap", "setCookie", "removeCookie", "jsonBody", "uploadFileInfo"), functions.keySet());
        assertSame(this.source, this.source.get(WebUdfSource.class));
    }

    @Test
    void reflectiveRegistrationInjectsHintsAndTheCompleteCookieArguments() throws Throwable {
        Map<String, Udf> functions = this.source.getUdfResource(new HostConfiguration()).get();
        RecordingWebResponse response = new RecordingWebResponse();
        HintsSet hints = new HintsSet();
        hints.setHint(WebUdfSource.HINT_RESPONSE, response);

        assertEquals(true, functions.get("setCookie").call(hints, () -> new Object[] { "session", "value", Map.of("HTTPONLY", true) }));
        assertEquals(true, functions.get("removeCookie").call(hints, () -> new Object[] { "old", Map.of("PATH", "/console") }));
        response.write(200, Map.of());

        assertEquals(List.of("session=value; Path=/; HttpOnly", "old=; Path=/console; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"), response.getHeaders().get("Set-Cookie"));
    }

    @Test
    void realScriptsReceiveParsedBodyAndAllReadFunctionShapes() throws Exception {
        Query query = new QueryManager(new HostConfiguration()).newBuilder().createQuery(this.script("""
                return {
                    "header": web.header('X-DEMO'),
                    "headers": web.headerArray('x-demo'),
                    "headerMap": web.headerMap(),
                    "headerArrays": web.headerArrayMap(),
                    "cookie": web.cookie('SESSION'),
                    "cookies": web.cookieArray('session'),
                    "cookieMap": web.cookieMap(),
                    "cookieArrays": web.cookieArrayMap(),
                    "body": web.jsonBody()
                };
                """));
        query.setHint(WebUdfSource.HINT_REQUEST, Map.of("headerValues", Map.of("x-demo", List.of("one", "two")), "cookies", Map.of("session", List.of("first", "second")), "body", Map.of("message", "body")));

        assertEquals(Map.of("header", "one", "headers", List.of("one", "two"), "headerMap", Map.of("x-demo", "one"), "headerArrays", Map.of("x-demo", List.of("one", "two")), "cookie", "first", "cookies", List.of("first", "second"), "cookieMap", Map.of("session", "first"), "cookieArrays", Map.of("session", List.of("first", "second")), "body", Map.of("message", "body")), query.execute().getData().unwrap());
    }

    private String script(String body) {
        return "import '" + WebUdfSource.class.getName() + "' as web;\n" + body;
    }

    @Test
    void datawayExecutionBindsWebFunctionsWithoutTreatingHintsAsScriptParameters() throws Exception {
        DatawayQuery query = this.query(new HostConfiguration(), """
                run web.setHeader('X-Request', web.header('X-Request'));
                run web.addHeader('X-Request', 'extra');
                run web.setHeaderAll({'X-Count': 2, 'X-Flag': true});
                run web.addHeaderAll({'X-Count': [3, 4]});
                run web.setCookie('session', web.cookie('session'), {'HttpOnly': true, 'SameSite': 'lax'});
                run web.removeCookie('old');
                return [web.jsonBody(), ${name}];
                """);
        RecordingWebResponse response = new RecordingWebResponse();
        Map<String, Object> request = Map.of("headers", Map.of("X-Request", "request-id"), "cookies", Map.of("session", "cookie-id"), "body", Map.of("bodyOnly", "value"));

        Object result = query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of("name", "parameter"), request, response).getData();
        assertEquals(List.of(Map.of("bodyOnly", "value"), "parameter"), result);
        assertFalse(response.isCommitted());
        response.write(200, Map.of());
        assertEquals(List.of("request-id", "extra"), response.getHeaders().get("X-Request"));
        assertEquals(List.of("2", "3", "4"), response.getHeaders().get("X-Count"));
        assertEquals(List.of("true"), response.getHeaders().get("X-Flag"));
        assertEquals(List.of("session=cookie-id; Path=/; HttpOnly; SameSite=Lax", "old=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"), response.getHeaders().get("Set-Cookie"));
    }

    private DatawayQuery query(HostConfiguration host, String script) throws Exception {
        host.addImport(WebUdfSource.class.getName(), () -> this.source);
        BeanContainer beans = new BeanContainer();
        beans.setBean(HostContext.class, host);
        beans.setBean(CustomizeScope.class, symbol -> Map.of());
        DatawayEngine engine = new DatawayEngine(beans, List.of());
        engine.setWrapParameterName("root");
        engine.setResultHandlers(new DatawayConfig().getResultHandlers());
        engine.setResultHandler("raw");

        ApiDefinition definition = new ApiDefinition();
        definition.setId("web-functions");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(this.script(script));
        return engine.newQuery(definition, null, List.of("name"), Map.of());
    }

    @Test
    void repeatedExecutionsCannotReadThePreviousRequestsContext() throws Exception {
        DatawayQuery query = this.query(new HostConfiguration(), "return [web.header('X-Request'), web.cookie('session'), web.jsonBody()];");
        Map<String, Object> first = Map.of("headers", Map.of("X-Request", "first"), "cookies", Map.of("session", "one"), "body", Map.of("request", "first"));
        Map<String, Object> second = Map.of("headers", Map.of("X-Request", "second"), "cookies", Map.of("session", "two"), "body", Map.of("request", "second"));

        assertEquals(List.of("first", "one", Map.of("request", "first")), query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), first, new RecordingWebResponse()).getData());
        assertEquals(List.of("second", "two", Map.of("request", "second")), query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), second, new RecordingWebResponse()).getData());
        Object absent = query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), Map.of(), new RecordingWebResponse()).getData();
        assertEquals("[null,null,null]", JsonUtils.writeValueAsString(absent));
    }

    @Test
    void aFailedExecutionDoesNotLeaveItsResponseBoundToTheNextCall() throws Exception {
        DatawayQuery query = this.query(new HostConfiguration(), "run web.setHeader('X-Request', 'value'); return 'done';");
        RecordingWebResponse first = new RecordingWebResponse();
        first.commit();
        Object failed = query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), Map.of(), first).getData();
        String message = assertInstanceOf(String.class, failed);
        assertTrue(message.contains("Response already started"), message);

        RecordingWebResponse next = new RecordingWebResponse();
        assertEquals("done", query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), Map.of(), next).getData());
        next.write(200, Map.of());
        assertEquals(List.of("value"), next.getHeaders().get("X-Request"));
        assertTrue(first.getHeaders().isEmpty());
    }

    @Test
    @Timeout(30)
    void concurrentExecutionsKeepRequestAndResponseBindingsSeparate() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(4);
        HostConfiguration host = new HostConfiguration();
        host.addImport("rendezvous", () -> (Udf) (hints, params) -> {
            barrier.await(10, TimeUnit.SECONDS);
            return null;
        });
        DatawayQuery query = this.query(host, """
                import 'rendezvous' as rendezvous;
                var before = web.header('X-Request');
                run rendezvous();
                run web.setHeader('X-Request', web.header('X-Request'));
                run web.setCookie('session', web.cookie('session'));
                return [before, web.header('X-Request'), web.jsonBody()];
                """);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                String token = "request-" + i;
                calls.add(executor.submit(() -> {
                    RecordingWebResponse response = new RecordingWebResponse();
                    Map<String, Object> request = Map.of("headers", Map.of("X-Request", token), "cookies", Map.of("session", token), "body", Map.of("request", token));
                    Object result = query.execute(Operation.INVOKE, UserIdentity.anonymous(Map.of()), ApiCallSource.HTTP, Map.of(), request, response).getData();
                    assertEquals(List.of(token, token, Map.of("request", token)), result);
                    response.write(200, Map.of());
                    assertEquals(List.of(token), response.getHeaders().get("X-Request"));
                    assertEquals(List.of("session=" + token + "; Path=/"), response.getHeaders().get("Set-Cookie"));
                    return null;
                }));
            }
            for (Future<?> call : calls) {
                call.get(15, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
