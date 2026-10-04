/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiHandlerTest extends ServiceTestSupport {
    @BeforeEach
    void provideApiIdentity() {
        this.config.identityProvider(r -> UserIdentity.authenticated("caller", Map.of()));
    }

    @Test
    void onlyTheNewestActiveReleaseForTheRequestedMethodAndPathIsExecuted() throws Exception {
        Map<FieldDef, String> old = this.release(this.info("api", "1", 1), "old", "1", 1);
        Map<FieldDef, String> newest = this.release(this.info("api", "1", 1), "new", "1", 2);
        newest.put(SCRIPT, "return 'newest';");
        when(this.access.listObjects(EntityType.RELEASE, Map.of(METHOD, "GET", PATH, "/api", STATUS, "1"))).thenReturn(List.of(newest, old));
        this.config.defaultResultHandler("raw");
        assertEquals("newest", this.handle(this.config.createDataway().getApiHandler(), "get", "/api").json());
        verify(this.access, never()).listObjects(eq(EntityType.INFO), anyMap());
        verify(this.access, never()).write(anyList());
    }

    @Test
    void bodyParametersOverrideQueryValuesWhileWebFunctionsOnlySeeTheBusinessBody() throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(METHOD, "POST");
        release.put(SCRIPT, """
                import 'net.hasor.dataway.function.WebUdfSource' as web;
                return [${name}, ${repeated}, ${flag}, ${encoded}, web.jsonBody(), web.header('Authorization'), web.cookie('session')];
                """);
        this.publishRoute(release);
        this.config.defaultResultHandler("raw");
        MemoryRequest request = this.request("POST", "/api");
        request.setQuery("name=query&repeated=one&repeated=two&repeated=three&flag&encoded=a%2Bb+c");
        request.json(Map.of("name", "body"));
        request.setHeaders(Map.of("Content-Type", "application/json", "Authorization", "secret", "Cookie", "session=token"));
        MemoryResponse response = new MemoryResponse();
        this.config.createDataway().getApiHandler().handle(request, response);
        assertEquals("[\"body\",[\"one\",\"two\",\"three\"],\"\",\"a+b c\",{\"name\":\"body\"},null,\"token\"]", response.text());
        assertTrue(request.isClosed());
    }

    @Test
    void rejectedAndMissingApisNeverExecuteInterceptorsAndCloseRequestResources() throws Exception {
        Closeable resource = mock(Closeable.class);
        MemoryRequest request = this.request("POST", "/api");
        request.resource(resource);
        this.config.authorizationCheck((identity, operation) -> false).apiInterceptor((context, chain) -> {
            fail("Rejected requests must not execute");
            return null;
        });
        MemoryResponse response = new MemoryResponse();
        assertEquals(401, assertThrows(DatawayException.class, () -> this.config.createDataway().getApiHandler().handle(request, response)).status());
        verify(resource).close();
        assertEquals(0, request.getReads());
        assertFalse(response.isStarted());
        this.config.authorizationCheck((identity, operation) -> true);
        assertEquals(404, assertThrows(DatawayException.class, () -> this.handle(this.config.createDataway().getApiHandler(), "GET", "/missing")).status());
    }

    @Test
    void anEmptyMountedPathResolvesToTheRootApiAndKeepsTheResolvedIdentity() throws Exception {
        UserIdentity identity = UserIdentity.authenticated("caller", Map.of());
        Map<FieldDef, String> release = this.release(this.info("root", "1", 1), "release", "1", 1);
        release.put(PATH, "/");
        this.publishRoute(release);
        this.config.defaultResultHandler("raw").identityProvider(request -> identity).apiInterceptor((context, chain) -> {
            assertEquals(ApiCallSource.HTTP, context.source());
            assertEquals(Operation.INVOKE, context.operation());
            assertSame(identity, context.identity());
            assertEquals("root", context.definition().getId());
            return chain.proceed(context);
        });
        assertEquals("value", this.handle(this.config.createDataway().getApiHandler(), "GET", "").json());
    }

    @Test
    void anonymousHttpCallsKeepTheirSourceRegardlessOfBusinessParameters() throws Exception {
        this.publishRoute(this.release(this.info("api", "1", 1), "release", "1", 1));
        this.config.defaultResultHandler("raw").identityProvider(request -> UserIdentity.anonymous(Map.of()));
        this.config.authorizationCheck((identity, operation) -> true);
        this.config.apiInterceptor((context, chain) -> {
            assertEquals(ApiCallSource.HTTP, context.source());
            assertFalse(context.identity().authenticated());
            assertEquals(Operation.INVOKE, context.operation());
            assertEquals("PROGRAMMATIC", context.parameters().get("source"));
            return chain.proceed(context);
        });
        MemoryRequest request = this.request("GET", "/api");
        request.json(Map.of("source", "PROGRAMMATIC"));
        MemoryResponse response = new MemoryResponse();
        this.config.createDataway().getApiHandler().handle(request, response);
        assertEquals("value", response.json());
    }

    @ParameterizedTest
    @ValueSource(strings = { "{bad", "[]", "3" })
    void invalidStoredOptionsFailBeforeRunningTheQuery(String options) {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(OPTION, options);
        this.publishRoute(release);
        assertEquals(400, assertThrows(DatawayException.class, () -> this.handle(this.config.createDataway().getApiHandler(), "GET", "/api")).status());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "null", "{}" })
    void absentOptionsUseTheEngineDefaults(String options) throws Exception {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(OPTION, options);
        this.publishRoute(release);
        this.config.defaultResultHandler("raw");
        assertEquals("value", this.handle(this.config.createDataway().getApiHandler(), "GET", "/api").json());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "{}", "{\"requestBody\":null}" })
    void missingSampleParameterDefinitionsDoNotInventFragmentArguments(String sample) throws Exception {
        AtomicReference<Map<String, Object>> parameters = new AtomicReference<>();
        this.config.defaultResultHandler("raw").fragment(ApiScriptType.SQL.getTypeName(), () -> (hints, values, script) -> {
            parameters.set(values);
            return "sql-result";
        });
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(TYPE, "SQL");
        release.put(SCRIPT, "select 1");
        release.put(SAMPLE, sample);
        this.publishRoute(release);
        assertEquals("sql-result", this.handle(this.config.createDataway().getApiHandler(), "GET", "/api").json());
        assertEquals(Map.of(), parameters.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void fragmentParameterNamesComeFromObjectOrLegacyStringSamples(boolean legacyString) throws Exception {
        this.config.defaultResultHandler("raw").fragment(ApiScriptType.SQL.getTypeName(), () -> (hints, values, script) -> values);
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(TYPE, "SQL");
        release.put(SCRIPT, "select :id");
        Object body = legacyString ? "{\"id\":0}" : Map.of("id", 0);
        release.put(SAMPLE, JsonUtils.writeValueAsString(Map.of("requestBody", body)));
        this.publishRoute(release);
        MemoryRequest request = this.request("GET", "/api");
        request.setQuery("id=7&unused=8");
        MemoryResponse response = new MemoryResponse();
        this.config.createDataway().getApiHandler().handle(request, response);
        assertEquals(Map.of("id", "7"), response.json());
    }

    @Test
    void invalidSampleBodiesAndMalformedQueryEscapesAreClientErrors() {
        Map<FieldDef, String> release = this.release(this.info("api", "1", 1), "release", "1", 1);
        release.put(SAMPLE, "{\"requestBody\":[]}");
        this.publishRoute(release);
        Dataway dataway = this.config.createDataway();
        assertEquals(400, assertThrows(DatawayException.class, () -> this.handle(dataway.getApiHandler(), "GET", "/api")).status());
        MemoryRequest request = this.request("GET", "/api");
        request.setQuery("value=%GG");
        assertEquals(400, assertThrows(DatawayException.class, () -> dataway.getApiHandler().handle(request, new MemoryResponse())).status());
    }
}
