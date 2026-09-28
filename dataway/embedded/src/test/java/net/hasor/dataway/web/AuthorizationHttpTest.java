/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.config.ServiceTestSupport;
import net.hasor.dataway.web.support.HttpTestServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class AuthorizationHttpTest extends ServiceTestSupport {
    @ParameterizedTest
    @MethodSource("identities")
    void everyPresetCanInvokeAPublishedApiWithItsIdentity(UserIdentity identity) throws Exception {
        AtomicInteger executions = new AtomicInteger();
        this.config.resultStructure(false).identityProvider(request -> identity).apiInterceptor((context, chain) -> {
            assertSame(identity, context.identity());
            assertEquals(Operation.INVOKE, context.operation());
            executions.incrementAndGet();
            return chain.proceed(context);
        });
        this.publishRoute(this.release(this.info("api", "1", 1), "release", "1", 1));
        try (HttpTestServer server = new HttpTestServer("/api", this.config.createDataway().getApiHandler())) {
            HttpResponse<String> response = server.send("GET", "/api/api", null, new byte[0]);
            assertEquals(200, response.statusCode(), response.body());
            assertEquals("value", JsonUtils.readValue(response.body(), String.class));
        }
        assertEquals(1, executions.get());
    }

    static Stream<UserIdentity> identities() {
        return Stream.of(UserIdentity.authenticated("caller", Map.of()), UserIdentity.consoleReadOnly("reader", Map.of()), UserIdentity.consoleAdmin("developer", Map.of()));
    }

    @Test
    void defaultIdentityCannotInvokePublishedApisOrReadConsoleOrSpecifications() throws Exception {
        this.config.resultStructure(false);
        this.publishRoute(this.release(this.info("api", "1", 1), "release", "1", 1));
        Dataway dataway = this.config.createDataway();
        try (HttpTestServer api = new HttpTestServer("/api", dataway.getApiHandler()); HttpTestServer admin = new HttpTestServer("/console", dataway.getAdminHandler()); HttpTestServer documents = new HttpTestServer("/docs", dataway.getDocumentHandler())) {
            assertEquals(401, api.send("GET", "/api/api", null, new byte[0]).statusCode());
            assertEquals(401, admin.send("GET", "/console/api-list", null, new byte[0]).statusCode());
            assertEquals(401, documents.send("GET", "/docs/openapi.json", null, new byte[0]).statusCode());
        }
        verify(this.access, never()).listObjects(any(), anyMap());
    }

    @Test
    void apiAccessDoesNotMakeDraftsOrDisabledReleasesCallable() throws Exception {
        this.config.identityProvider(request -> UserIdentity.authenticated("caller", Map.of()));
        Map<FieldDef, String> draft = this.info("api", "0", 1);
        this.storeInfo(draft);
        Map<FieldDef, String> filter = Map.of(METHOD, "GET", PATH, "/api", STATUS, "1");
        Map<FieldDef, String> release = this.release(draft, "release", "1", 1);
        when(this.access.listObjects(EntityType.RELEASE, filter)).thenReturn(List.of(), List.of(release), List.of());
        AtomicInteger executions = new AtomicInteger();
        this.config.apiInterceptor((context, chain) -> {
            executions.incrementAndGet();
            return chain.proceed(context);
        });
        try (HttpTestServer server = new HttpTestServer("/api", this.config.createDataway().getApiHandler())) {
            assertEquals(404, server.send("GET", "/api/api", null, new byte[0]).statusCode());
            assertEquals(200, server.send("GET", "/api/api", null, new byte[0]).statusCode());
            assertEquals(404, server.send("GET", "/api/api", null, new byte[0]).statusCode());
        }
        assertEquals(1, executions.get());
        verify(this.access, times(3)).listObjects(EntityType.RELEASE, filter);
        verify(this.access, never()).getObject(eq(EntityType.INFO), anyString());
    }

    @ParameterizedTest
    @CsvSource({ "GET, /api-list,    LIST,    true", "GET, /api-info,    READ,    true", "GET, /api-detail,  READ,    true", "GET, /api-history, HISTORY, true", "GET, /get-history, HISTORY, true", "POST,/save-api,    SAVE,    false", "POST,/publish,     PUBLISH, false", "POST,/disable,     DISABLE, false", "POST,/delete,      DELETE,  false", "POST,/perform,     DEBUG,   false", "POST,/smoke,       DEBUG,   false" })
    void managementRoutesAuthorizeEachRequestBeforeBodyParsingAndInterception(String method, String path, Operation operation, boolean readOnly) throws Exception {
        AtomicReference<UserIdentity> current = new AtomicReference<>();
        AtomicInteger intercepted = new AtomicInteger();
        this.config.identityProvider(request -> current.get()).adminInterceptor((context, chain) -> {
            assertSame(current.get(), context.identity());
            assertEquals(operation, context.operation());
            intercepted.incrementAndGet();
            return Map.of("operation", context.operation().name());
        });
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            Map<UserIdentity, Boolean> identities = Map.of(UserIdentity.anonymous(Map.of()), false, UserIdentity.authenticated("caller", Map.of()), false, UserIdentity.consoleReadOnly("reader", Map.of()), readOnly, UserIdentity.consoleAdmin("developer", Map.of()), true);
            for (var entry : identities.entrySet()) {
                current.set(entry.getKey());
                // A denied request must not reach JSON parsing or the response-replacing interceptor.
                HttpResponse<String> response = server.send(method, "/console" + path, "application/json", "{broken".getBytes(StandardCharsets.UTF_8));
                boolean allowed = entry.getValue();
                assertEquals(allowed ? 200 : 401, response.statusCode(), current.get().identityId() + ": " + response.body());
                if (allowed) {
                    assertEquals(Map.of("operation", operation.name()), JsonUtils.readValue(response.body(), Map.class));
                }
            }
        }
        assertEquals(readOnly ? 2 : 1, intercepted.get());
        verify(this.access, never()).listObjects(any(), anyMap());
        verify(this.access, never()).write(anyList());
    }

    @ParameterizedTest
    @CsvSource({ "GET,/swagger2.json", "GET,/openapi.json", "HEAD,/swagger2.json", "HEAD,/openapi.json" })
    void specificationsAreReadOnlyOperations(String method, String path) throws Exception {
        AtomicReference<UserIdentity> current = new AtomicReference<>();
        this.config.identityProvider(request -> current.get());
        try (HttpTestServer server = new HttpTestServer("/docs", this.config.createDataway().getDocumentHandler())) {
            Map<UserIdentity, Integer> identities = Map.of(UserIdentity.anonymous(Map.of()), 401, UserIdentity.authenticated("caller", Map.of()), 200, UserIdentity.consoleReadOnly("reader", Map.of()), 200, UserIdentity.consoleAdmin("developer", Map.of()), 200);
            for (var entry : identities.entrySet()) {
                current.set(entry.getKey());
                HttpResponse<String> response = server.send(method, "/docs" + path, null, new byte[0]);
                assertEquals(entry.getValue().intValue(), response.statusCode(), current.get().identityId() + ": " + response.body());
            }
        }
    }

    @Test
    void hostCanRestrictPresetPermissionsThroughAuthorizationCheck() throws Exception {
        UserIdentity identity = UserIdentity.consoleReadOnly("auditor", Map.of("tenant", "example"));
        this.config.identityProvider(request -> identity).authorizationCheck((user, operation) -> user.checkOperation(operation) && operation == Operation.LIST);
        this.config.adminInterceptor((context, chain) -> {
            assertSame(identity, context.identity());
            return Map.of("tenant", context.identity().attributes().get("tenant"));
        });
        Dataway dataway = this.config.createDataway();
        try (HttpTestServer admin = new HttpTestServer("/console", dataway.getAdminHandler()); HttpTestServer api = new HttpTestServer("/api", dataway.getApiHandler()); HttpTestServer docs = new HttpTestServer("/docs", dataway.getDocumentHandler())) {
            HttpResponse<String> listed = admin.send("GET", "/console/api-list", null, new byte[0]);
            assertEquals(200, listed.statusCode());
            assertEquals(Map.of("tenant", "example"), JsonUtils.readValue(listed.body(), Map.class));
            assertEquals(401, admin.send("GET", "/console/api-detail?id=one", null, new byte[0]).statusCode());
            assertEquals(401, admin.send("POST", "/console/delete", "application/json", "{broken".getBytes(StandardCharsets.UTF_8)).statusCode());
            assertEquals(401, api.send("GET", "/api/one", null, new byte[0]).statusCode());
            assertEquals(401, docs.send("GET", "/docs/openapi.json", null, new byte[0]).statusCode());
        }
        verify(this.access, never()).listObjects(any(), anyMap());
        verify(this.access, never()).write(anyList());
    }

}
