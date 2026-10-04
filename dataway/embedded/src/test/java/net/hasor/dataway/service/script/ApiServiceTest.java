/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiServiceTest extends ServiceTestSupport {
    private final UserIdentity caller = UserIdentity.authenticated("application", Map.of("tenant", "example"));

    @Test
    void javaAndHttpCallsUseTheSamePublishedScriptAndOptions() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("people", "1", 1), "release-1", "1", 1);
        release.put(METHOD, "POST");
        release.put(SCRIPT, "return {'name': ${name}, 'message': 'published'};");
        release.put(OPTION, "{\"resultHandler\":\"raw\"}");
        this.publishRoute(release);
        this.config.identityProvider(request -> this.caller);
        Dataway dataway = this.config.createDataway();

        ResultInfo javaResult = dataway.getApiService().invokeByPath("POST", "/people", Map.of("name", "Alice"));
        MemoryRequest request = this.request("POST", "/people");
        request.json(Map.of("name", "Alice"));
        MemoryResponse httpResult = new MemoryResponse();
        dataway.getApiHandler().handle(request, httpResult);

        assertEquals(Map.of("name", "Alice", "message", "published"), javaResult.getData());
        assertEquals(javaResult.getData(), httpResult.json());
        assertEquals(javaResult.getStatus(), httpResult.getStatus());
        assertEquals(List.of(javaResult.getHeaders().get("Content-Type")), httpResult.getHeaders().get("Content-Type"));
    }

    @Test
    void methodsIgnoreCaseButPathsRemainCaseSensitiveAndMethodSpecific() throws Exception {
        this.config.defaultResultHandler("raw");
        Map<FieldDef, String> get = this.release(this.info("people", "1", 1), "get-release", "1", 1);
        get.put(SCRIPT, "return 'get';");
        this.publishRoute(get);
        Map<FieldDef, String> post = this.release(this.info("people-post", "1", 1), "post-release", "1", 1);
        post.put(METHOD, "POST");
        post.put(PATH, "/people");
        post.put(SCRIPT, "return 'post';");
        this.publishRoute(post);
        ApiService service = this.config.createDataway().getApiService();

        assertEquals("get", service.invokeByPath("get", "/people", Map.of()).getData());
        assertEquals("post", service.invokeByPath("pOsT", "/people", Map.of()).getData());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeByPath("GET", "/People", Map.of())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeByPath("DELETE", "/people", Map.of())).status());
    }

    @Test
    void newestPublishedSnapshotIsUsedWithoutReadingTheDraft() throws Exception {
        this.config.defaultResultHandler("raw");
        Map<FieldDef, String> draft = this.info("api", "1", 8);
        draft.put(SCRIPT, "return 'unpublished';");
        this.storeInfo(draft);
        Map<FieldDef, String> old = this.release(draft, "release-1", "1", 1);
        old.put(SCRIPT, "return 'old';");
        Map<FieldDef, String> latest = this.release(draft, "release-2", "1", 2);
        latest.put(SCRIPT, "return 'latest';");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(METHOD, "GET", PATH, "/api", STATUS, "1"))).thenReturn(List.of(latest, old));
        AtomicReference<String> releaseID = new AtomicReference<>();
        this.config.apiInterceptor((context, chain) -> {
            releaseID.set(context.releaseId());
            assertEquals("api", context.definition().getId());
            assertEquals("return 'latest';", context.definition().getScript());
            return chain.proceed(context);
        });

        assertEquals("latest", this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of()).getData());
        assertEquals("release-2", releaseID.get());
        verify(this.access, never()).getObject(eq(EntityType.INFO), anyString());
        verify(this.access, never()).listObjects(eq(EntityType.INFO), anyMap());
        verify(this.access, never()).write(anyList());
    }

    @Test
    void disabledAndMissingApisFailWithoutExecutingInterceptors() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        this.publishRoute(release);
        this.config.defaultResultHandler("raw");
        ApiInterceptor interceptor = mock(ApiInterceptor.class);
        this.config.apiInterceptor(interceptor);
        when(interceptor.invoke(any(), any())).thenAnswer(invocation -> {
            ApiInterceptorContext context = invocation.getArgument(0);
            ApiInterceptorChain chain = invocation.getArgument(1);
            return chain.proceed(context);
        });
        ApiService service = this.config.createDataway().getApiService();
        assertEquals("value", service.invokeByPath("GET", "/api", Map.of()).getData());
        clearInvocations(interceptor);

        when(this.access.listObjects(EntityType.RELEASE, Map.of(METHOD, "GET", PATH, "/api", STATUS, "1"))).thenReturn(List.of());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeByPath("GET", "/api", Map.of())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeByPath("GET", "/missing", Map.of())).status());
        verifyNoInteractions(interceptor);
    }

    @Test
    void javaCallsDoNotResolveIdentityOrCheckHttpAuthorization() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        this.publishRoute(release);
        when(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, "api", STATUS, "1"))).thenReturn(List.of(release));
        this.config.defaultResultHandler("raw");
        this.config.identityProvider(request -> {
            return fail("Java calls do not resolve HTTP identities");
        });
        this.config.authorizationCheck((identity, operation) -> {
            return fail("Java calls do not run HTTP authorization checks");
        });
        ApiService service = this.config.createDataway().getApiService();

        assertEquals("value", service.invokeByPath("GET", "/api", Map.of()).getData());
        assertEquals("value", service.invokeById("api", Map.of()).getData());
    }

    @Test
    void javaCallsDoNotChangeHttpAuthorization() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        this.publishRoute(release);
        this.config.defaultResultHandler("raw").identityProvider(request -> this.caller);
        this.config.authorizationCheck((identity, operation) -> {
            assertSame(this.caller, identity);
            assertEquals(Operation.INVOKE, operation);
            return false;
        });
        Dataway dataway = this.config.createDataway();
        assertEquals("value", dataway.getApiService().invokeByPath("GET", "/api", Map.of()).getData());
        clearInvocations(this.access);

        MemoryRequest request = this.request("GET", "/api");
        request.json(Map.of("name", "Alice"));
        MemoryResponse response = new MemoryResponse();
        assertEquals(401, assertThrows(DatawayException.class, () -> dataway.getApiHandler().handle(request, response)).status());
        assertEquals(0, request.getReads());
        assertTrue(request.isClosed());
        assertFalse(response.isStarted());
        verifyNoInteractions(this.access);
    }

    @Test
    void invokingByIdUsesTheLatestEnabledPublicationAndSharesPathExecution() throws Exception {
        Map<FieldDef, String> draft = this.info("people", "1", 8);
        draft.put(METHOD, "POST");
        draft.put(PATH, "/people/lookup");
        draft.put(SCRIPT, "return 'unpublished';");
        this.storeInfo(draft);
        Map<FieldDef, String> older = this.release(draft, "release-1", "1", 1);
        older.put(SCRIPT, "return 'old';");
        Map<FieldDef, String> latest = this.release(draft, "release-2", "1", 2);
        latest.put(SCRIPT, "var parameters = ${root}; return parameters.name;");
        latest.put(OPTION, "{\"resultHandler\":\"text\",\"wrapAllParameters\":true,\"wrapParameterName\":\"root\"}");
        this.publishRoute(latest);
        when(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, "people", STATUS, "1"))).thenReturn(List.of(latest, older));
        this.config.customizeScope(symbol -> "$".equals(symbol) ? Map.of("name", "default", "fallback", "host") : Map.of());
        this.config.apiInterceptor((context, chain) -> {
            assertEquals(ApiCallSource.PROGRAMMATIC, context.source());
            assertFalse(context.identity().authenticated());
            assertTrue(context.identity().attributes().isEmpty());
            assertEquals(Operation.INVOKE, context.operation());
            assertEquals("people", context.definition().getId());
            assertEquals("release-2", context.releaseId());
            assertEquals("POST", context.definition().getMethod());
            assertEquals("/people/lookup", context.definition().getPath());
            assertEquals(Map.of("root", Map.of("name", "Alice", "fallback", "host")), context.parameters());
            return chain.proceed(context);
        });
        ApiService service = this.config.createDataway().getApiService();

        ResultInfo byId = service.invokeById("people", Map.of("name", "Alice"));
        ResultInfo byPath = service.invokeByPath("POST", "/people/lookup", Map.of("name", "Alice"));
        assertArrayEquals("Alice".getBytes(StandardCharsets.UTF_8), (byte[]) byId.getData());
        assertArrayEquals((byte[]) byPath.getData(), (byte[]) byId.getData());
        assertEquals(byPath.getHeaders(), byId.getHeaders());
        assertFalse(byId.isJson());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeById("release-2", Map.of())).status());
        verify(this.access).listObjects(EntityType.RELEASE, Map.of(API_ID, "people", STATUS, "1"));
        verify(this.access, never()).getObject(eq(EntityType.INFO), anyString());
        verify(this.access, never()).listObjects(eq(EntityType.INFO), anyMap());
    }

    @Test
    void invokingByIdRejectsDisabledAndUnpublishedApis() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        when(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, "api", STATUS, "1"))).thenReturn(List.of(release));
        this.storeInfo(this.info("unpublished", "0", 1));
        this.config.defaultResultHandler("raw");
        ApiService service = this.config.createDataway().getApiService();
        assertEquals("value", service.invokeById("api", Map.of()).getData());

        when(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, "api", STATUS, "1"))).thenReturn(List.of());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeById("api", Map.of())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeById("unpublished", Map.of())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.invokeById("missing", Map.of())).status());
        verify(this.access, never()).getObject(eq(EntityType.INFO), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t" })
    void invokingByIdRequiresANonBlankApiId(String apiID) {
        ApiService service = this.config.createDataway().getApiService();
        clearInvocations(this.access);

        assertEquals(400, assertThrows(DatawayException.class, () -> service.invokeById(apiID, Map.of())).status());
        verifyNoInteractions(this.access);
    }

    @Test
    void interceptorsReceiveTheTargetReleaseAndParametersWithoutARequestIdentity() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release-9", "1", 9);
        release.put(SCRIPT, "return ${root};");
        release.put(OPTION, "{\"resultHandler\":\"raw\",\"wrapAllParameters\":true,\"wrapParameterName\":\"root\"}");
        this.publishRoute(release);
        this.config.customizeScope(symbol -> "$".equals(symbol) ? Map.of("name", "default", "fallback", "host") : Map.of());
        this.config.apiInterceptor((context, chain) -> {
            assertEquals(ApiCallSource.PROGRAMMATIC, context.source());
            assertFalse(context.identity().authenticated());
            assertTrue(context.identity().attributes().isEmpty());
            assertEquals(Operation.INVOKE, context.operation());
            assertEquals("api", context.definition().getId());
            assertEquals("GET", context.definition().getMethod());
            assertEquals("/api", context.definition().getPath());
            assertEquals("release-9", context.releaseId());
            assertEquals(Map.of("root", Map.of("name", "request", "fallback", "host")), context.parameters());
            return chain.proceed(context);
        });
        Map<String, String> parameters = Map.of("name", "request");

        ResultInfo result = this.config.createDataway().getApiService().invokeByPath("GET", "/api", parameters);
        assertEquals(Map.of("name", "request", "fallback", "host"), result.getData());
        assertEquals(Map.of("name", "request"), parameters);
    }

    @Test
    void anExplicitInterceptorResponseIsReturnedWithoutRunningTheScript() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(SCRIPT, "throw 500, 'must not execute';");
        this.publishRoute(release);
        ResultInfo response = ResultInfoUtils.json(403, Map.of("reason", "API unavailable for this tenant"));
        this.config.apiInterceptor((context, chain) -> response);

        assertSame(response, this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of()));
    }

    @Test
    void ordinaryInterceptorResultsKeepTheirExistingUnwrappedContract() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(SCRIPT, "throw 500, 'must not execute';");
        release.put(OPTION, "{\"resultHandler\":\"text\"}");
        this.publishRoute(release);
        Map<String, String> value = Map.of("source", "interceptor");
        this.config.apiInterceptor((context, chain) -> value);

        ResultInfo result = this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of());
        assertEquals(value, result.getData());
        assertTrue(result.isJson());
    }

    @Test
    void thrownInterceptorErrorsUseTheExistingFailureResponse() throws Exception {
        this.publishRoute(this.release(this.info("api", "1", 1), "release", "1", 1));
        this.config.apiInterceptor((context, chain) -> {
            throw new IllegalStateException("Access denied for this API");
        });

        ResultInfo result = this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of());
        Map<?, ?> failure = (Map<?, ?>) result.getData();
        assertEquals(false, failure.get("success"));
        assertEquals(500, failure.get("code"));
        assertEquals("Access denied for this API", failure.get("message"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void sqlFragmentArgumentsComeFromThePublishedSample(boolean legacyString) throws Exception {
        this.config.defaultResultHandler("raw").fragment(ApiScriptType.SQL.getTypeName(), () -> (hints, values, script) -> values);
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(TYPE, "SQL");
        release.put(SCRIPT, "select :id");
        Object sample = legacyString ? "{\"id\":0}" : Map.of("id", 0);
        release.put(SAMPLE, JsonUtils.writeValueAsString(Map.of("requestBody", sample)));
        this.publishRoute(release);

        ResultInfo result = this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of("id", "7", "unused", "8"));
        assertEquals(Map.of("id", "7"), result.getData());
    }

    @Test
    void aSelectedTextHandlerReturnsItsBytesAndContentType() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(SCRIPT, "return 'Hello Dataway';");
        release.put(OPTION, "{\"resultHandler\":\"text\"}");
        this.publishRoute(release);

        ResultInfo result = this.config.createDataway().getApiService().invokeByPath("GET", "/api", Map.of());
        assertEquals(200, result.getStatus());
        assertFalse(result.isJson());
        assertEquals("text/plain; charset=UTF-8", result.getHeaders().get("Content-Type"));
        assertArrayEquals("Hello Dataway".getBytes(StandardCharsets.UTF_8), (byte[]) result.getData());
    }

    @Test
    void nullParametersUseScopeDefaultsWithoutCreatingAWebRequest() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(SCRIPT, "return ${name};");
        this.publishRoute(release);
        this.config.defaultResultHandler("raw").customizeScope(symbol -> "$".equals(symbol) ? Map.of("name", "default") : Map.of());
        this.config.identityProvider(request -> {
            fail("Java calls must not create a WebRequest");
            return null;
        });

        assertEquals("default", this.config.createDataway().getApiService().invokeByPath("GET", "/api", null).getData());
    }
}
