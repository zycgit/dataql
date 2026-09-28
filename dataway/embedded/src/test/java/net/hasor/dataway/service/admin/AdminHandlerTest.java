/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminHandlerTest extends ServiceTestSupport {
    @Test
    void authorizationPrecedesBodyParsingInterceptionAndStorageAccess() {
        List<String> events = new ArrayList<>();
        this.config.authorizationCheck((identity, operation) -> {
            assertEquals(Operation.SAVE, operation);
            events.add("authorization");
            return false;
        }).adminInterceptor((context, chain) -> {
            events.add("interceptor");
            return chain.proceed();
        });
        Dataway dataway = this.config.createDataway();
        MemoryRequest request = this.request("POST", "/save-api");
        MemoryResponse response = new MemoryResponse();
        assertEquals(401, assertThrows(DatawayException.class, () -> dataway.getAdminHandler().handle(request, response)).status());
        assertEquals(List.of("authorization"), events);
        assertEquals(0, request.getReads());
        assertTrue(request.isClosed());
        assertFalse(response.isStarted());
        verify(this.access, never()).listObjects(any(), anyMap());
    }

    @Test
    void interceptorsRunInRegistrationOrderAroundTheControllerWithResolvedIdentity() throws Exception {
        List<String> events = new ArrayList<>();
        UserIdentity identity = UserIdentity.authenticated("admin");
        this.config.identityProvider(request -> identity);
        this.config.adminInterceptor((context, chain) -> {
            assertSame(identity, context.getIdentity());
            assertEquals(Operation.LIST, context.getOperation());
            assertNull(context.getDefinition());
            assertTrue(context.getParameters().isEmpty());
            events.add("first-before");
            Object result = chain.proceed();
            events.add("first-after");
            return result;
        }).adminInterceptor((context, chain) -> {
            events.add("second-before");
            Object result = chain.proceed();
            events.add("second-after");
            return result;
        });
        assertEquals(Map.of("success", true, "code", 200, "message", "OK", "result", List.of()), this.handle(this.config.createDataway().getAdminHandler(), "get", "/api-list").json());
        assertEquals(List.of("first-before", "second-before", "second-after", "first-after"), events);
    }

    @Test
    void anInterceptorCanReplaceTheResponseWithoutCallingTheService() throws Exception {
        this.config.adminInterceptor((context, chain) -> ResultInfoUtils.json(202, Map.of("queued", true)));
        MemoryResponse response = this.handle(this.config.createDataway().getAdminHandler(), "GET", "/api-list");
        assertEquals(202, response.getStatus());
        assertEquals(Map.of("queued", true), response.json());
        verify(this.access, never()).listObjects(any(), anyMap());
    }

    @Test
    void invalidRoutesMethodsAndServiceFailuresRemainHostExceptions() throws Exception {
        Dataway dataway = this.config.createDataway();
        assertEquals(404, assertThrows(DatawayException.class, () -> this.handle(dataway.getAdminHandler(), "GET", "/missing")).status());
        assertEquals(405, assertThrows(DatawayException.class, () -> this.handle(dataway.getAdminHandler(), "POST", "/api-list")).status());
        RuntimeException failure = new IllegalStateException("Storage unavailable");
        when(this.access.listObjects(EntityType.INFO, Map.of())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> this.handle(dataway.getAdminHandler(), "GET", "/api-list")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api-info", "/api-detail", "/api-history", "/get-history" })
    void readRoutesUseTheManagementServices(String path) throws Exception {
        Map<FieldDef, String> info = this.info("api", "1", 1);
        Map<FieldDef, String> release = this.release(info, "history", "1", 1);
        this.storeInfo(info);
        this.storeReleases("api", List.of(release));
        doReturn(Optional.of(release)).when(this.access).getObject(EntityType.RELEASE, "history");
        MemoryRequest request = this.request("GET", path);
        request.setQuery("id=api&historyId=history");
        MemoryResponse response = new MemoryResponse();
        this.config.createDataway().getAdminHandler().handle(request, response);
        assertEquals(200, response.getStatus());
        assertEquals(true, ((Map<?, ?>) response.json()).get("success"));
    }

    @Test
    void performUsesTheExecutionEngineAndDoesNotSaveTheEditorContents() throws Exception {
        MemoryRequest request = this.request("POST", "/perform");
        request.json(Map.of("id", "-1", "select", "POST", "apiPath", "/preview", "codeType", "DataQL", "codeValue", "return 'preview';"));
        MemoryResponse response = new MemoryResponse();
        this.config.resultStructure(false).createDataway().getAdminHandler().handle(request, response);
        assertEquals("preview", response.json());
        verify(this.access, never()).write(anyList());
    }
}
