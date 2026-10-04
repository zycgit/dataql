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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.admin.AdminInterceptorContext;
import net.hasor.dataway.service.config.ServiceTestSupport;
import net.hasor.dataway.web.support.HttpTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Resolves management targets through real HTTP requests before application interception. */
class AdminInterceptionHttpTest extends ServiceTestSupport {
    @BeforeEach
    void useDeveloperIdentity() {
        this.config.identityProvider(request -> UserIdentity.consoleAdmin("developer", Map.of("tenant", "example")));
    }

    @ParameterizedTest
    @CsvSource({ "GET,/api-info,READ", "GET,/api-detail,READ", "GET,/api-history,HISTORY", "POST,/publish,PUBLISH", "POST,/disable,DISABLE", "POST,/delete,DELETE", "POST,/smoke,DEBUG" })
    void storedTargetsCanBeAuthorizedBeforeManagementOrExecution(String method, String path, Operation operation) throws Exception {
        this.storeInfo(this.info("api", "1", 3));
        AtomicReference<AdminInterceptorContext> intercepted = new AtomicReference<>();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.set(context);
            throw new DatawayException(403, "API access denied");
        });

        HttpResponse<String> response = this.send(method, path + "?id=api", Map.of("version", 3));

        assertEquals(403, response.statusCode(), response.body());
        AdminInterceptorContext context = intercepted.get();
        assertNotNull(context);
        assertEquals(operation, context.operation());
        assertEquals("api", context.definition().getId());
        assertEquals("GET", context.definition().getMethod());
        assertEquals("/api", context.definition().getPath());
        assertEquals("return 'value';", context.definition().getScript());
        assertEquals("example", context.identity().attributes().get("tenant"));
        assertEquals("api", context.parameters().get("id"));
        assertNull(context.releaseId());
        verify(this.access, never()).write(anyList());
    }

    private HttpResponse<String> send(String method, String path, Map<String, ?> body) throws Exception {
        try (HttpTestServer server = new HttpTestServer("/console", this.config.createDataway().getAdminHandler())) {
            byte[] bytes = JsonUtils.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            return server.send(method, "/console" + path, "application/json", bytes);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "/save-api", "/perform" })
    void changingTheSubmittedPathDoesNotReplaceTheExistingAuthorizationTarget(String path) throws Exception {
        this.storeInfo(this.info("api", "1", 3));
        AtomicReference<AdminInterceptorContext> intercepted = new AtomicReference<>();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.set(context);
            return ResultInfoUtils.json(403, Map.of("denied", true));
        });

        HttpResponse<String> response = this.send("POST", path, this.editor("api"));

        assertEquals(403, response.statusCode());
        assertEquals("/api", intercepted.get().definition().getPath());
        assertEquals("/submitted", intercepted.get().parameters().get("apiPath"));
        assertEquals("return 'submitted';", intercepted.get().parameters().get("codeValue"));
        verify(this.access, never()).write(anyList());
    }

    private Map<String, Object> editor(String id) {
        return Map.of("id", id, "version", 0, "select", "POST", "apiPath", "/submitted", "codeType", "DataQL", "codeValue", "return 'submitted';");
    }

    @ParameterizedTest
    @ValueSource(strings = { "/save-api", "/perform" })
    void unsavedTargetsUseTheSubmittedDefinitionWithoutLookingUpTheSentinelId(String path) throws Exception {
        AtomicReference<AdminInterceptorContext> intercepted = new AtomicReference<>();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.set(context);
            return ResultInfoUtils.json(202, Map.of("accepted", true));
        });

        HttpResponse<String> response = this.send("POST", path, this.editor("-1"));

        assertEquals(202, response.statusCode(), response.body());
        assertEquals("-1", intercepted.get().definition().getId());
        assertEquals("POST", intercepted.get().definition().getMethod());
        assertEquals("/submitted", intercepted.get().definition().getPath());
        assertEquals("return 'submitted';", intercepted.get().definition().getScript());
        assertNull(intercepted.get().releaseId());
        verify(this.access, never()).getObject(any(), anyString());
        verify(this.access, never()).listObjects(any(), anyMap());
        verify(this.access, never()).write(anyList());
    }

    @Test
    void selectedHistoryExposesItsSnapshotAndPublicationId() throws Exception {
        Map<FieldDef, String> info = this.info("api", "1", 3);
        this.storeInfo(info);
        Map<FieldDef, String> release = this.release(info, "old", "3", 1);
        release.put(FieldDef.PATH, "/original");
        release.put(FieldDef.SCRIPT, "return 'history';");
        this.storeReleases("api", List.of(release));
        doReturn(Optional.of(release)).when(this.access).getObject(EntityType.RELEASE, "old");
        AtomicReference<AdminInterceptorContext> intercepted = new AtomicReference<>();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.set(context);
            return ResultInfoUtils.json(200, Map.of("checked", true));
        });

        HttpResponse<String> response = this.send("GET", "/get-history?id=api&historyId=old", Map.of());

        assertEquals(200, response.statusCode(), response.body());
        assertEquals("old", intercepted.get().releaseId());
        assertEquals("api", intercepted.get().definition().getId());
        assertEquals("/original", intercepted.get().definition().getPath());
        assertEquals("return 'history';", intercepted.get().definition().getScript());
    }

    @Test
    void anotherApisHistoryIsRejectedBeforeItsDefinitionReachesTheInterceptor() throws Exception {
        Map<FieldDef, String> info = this.info("other", "1", 1);
        this.storeInfo(info);
        Map<FieldDef, String> other = this.release(info, "other-history", "1", 1);
        this.storeReleases("other", List.of(other));
        doReturn(Optional.of(other)).when(this.access).getObject(EntityType.RELEASE, "other-history");
        AtomicInteger intercepted = new AtomicInteger();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.incrementAndGet();
            return chain.proceed();
        });

        HttpResponse<String> response = this.send("GET", "/get-history?id=api&historyId=other-history", Map.of());

        assertEquals(404, response.statusCode(), response.body());
        assertEquals(0, intercepted.get());
    }

    @Test
    void collectionOperationsHaveNoTargetAndRetainDecodedQueryParameters() throws Exception {
        AtomicReference<AdminInterceptorContext> intercepted = new AtomicReference<>();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.set(context);
            return chain.proceed();
        });

        HttpResponse<String> response = this.send("GET", "/api-list?tag=team%20one", Map.of());

        assertEquals(200, response.statusCode(), response.body());
        assertNull(intercepted.get().definition());
        assertNull(intercepted.get().releaseId());
        assertEquals(Map.of("tag", "team one"), intercepted.get().parameters());
    }

    @Test
    void malformedAndConflictingIdsCannotReachTargetLookupOrInterception() throws Exception {
        AtomicInteger intercepted = new AtomicInteger();
        this.config.adminInterceptor((context, chain) -> {
            intercepted.incrementAndGet();
            return chain.proceed();
        });

        assertEquals(400, this.send("POST", "/delete?id=api", Map.of("id", "other", "version", 0)).statusCode());
        assertEquals(400, this.send("POST", "/delete?id=api&id=other", Map.of("version", 0)).statusCode());
        assertEquals(0, intercepted.get());
        verify(this.access, never()).getObject(any(), anyString());
        verify(this.access, never()).listObjects(any(), anyMap());
        verify(this.access, never()).write(anyList());
    }
}
