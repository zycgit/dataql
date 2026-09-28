/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.web.body.UploadStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebHandlerTest extends ServiceTestSupport {
    private BeanContainer beans(IdentityProvider identity) {
        BeanContainer beans = new BeanContainer();
        beans.setBean(IdentityProvider.class, identity);
        beans.setBean(UploadStorage.class, UploadStorage.DEFAULT);
        return beans;
    }

    @Test
    void identityIsResolvedBeforeCallingTheServiceAndResourcesCloseAfterOutput() throws Exception {
        UserIdentity identity = UserIdentity.authenticated("user");
        MemoryRequest request = this.request("GET", "/");
        MemoryResponse response = new MemoryResponse();
        Closeable resource = mock(Closeable.class);
        request.resource(resource);
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> identity), (r, output) -> {
            assertSame(identity, r.getIdentity());
            assertFalse(request.isClosed());
            return ResultInfoUtils.buildSuccess("done", 7);
        });

        handler.handle(request, response);
        assertTrue(request.isClosed());
        verify(resource).close();
        assertEquals(Map.of("success", true, "code", 200, "message", "OK", "result", "done", "version", 7), response.json());
        assertEquals("no-store", response.getHeaders().get("Cache-Control").get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD" })
    void streamingResultsCloseTheirSourceEvenForHead(String method) throws Exception {
        ByteArrayInputStream source = spy(new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        ResultInfo result = ResultInfoUtils.convertToResultInfo("application/custom", source);
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, output) -> result);
        MemoryResponse response = this.handle(handler, method, "/");

        assertArrayEquals(method.equals("HEAD") ? new byte[0] : new byte[] { 1, 2, 3 }, response.bytes());
        assertEquals("application/custom", response.getHeaders().get("Content-Type").get(0));
        verify(source).close();
    }

    @Test
    void failedOutputStillClosesTheSourceAndRequestWithoutReplacingTheException() throws Exception {
        ByteArrayInputStream source = spy(new ByteArrayInputStream(new byte[] { 1 }));
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, output) -> ResultInfoUtils.convertToResultInfo(source));
        MemoryRequest request = this.request("GET", "/");
        MemoryResponse response = new MemoryResponse();
        IOException failure = new IOException("Disconnected");
        response.failWith(failure);
        assertSame(failure, assertThrows(IOException.class, () -> handler.handle(request, response)));
        verify(source).close();
        assertTrue(request.isClosed());
    }

    @Test
    void serviceFailuresAndIdentityFailuresPropagateBeforeAResponseStarts() {
        DatawayException failure = new DatawayException(409, "Conflict");
        MemoryRequest request = this.request("GET", "/");
        MemoryResponse response = new MemoryResponse();
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, output) -> {
            throw failure;
        });
        assertSame(failure, assertThrows(DatawayException.class, () -> handler.handle(request, response)));
        assertTrue(request.isClosed());
        assertFalse(response.isStarted());

        MemoryRequest invalidIdentity = this.request("GET", "/");
        ResultWebHandler invalid = new ResultWebHandler(this.beans(r -> null), (r, output) -> {
            fail("The service must not run without an identity");
            return null;
        });
        assertThrows(NullPointerException.class, () -> invalid.handle(invalidIdentity, response));
        assertTrue(invalidIdentity.isClosed());
    }

    @Test
    void cleanupFailuresAreSuppressedWithoutHidingTheOriginalServiceFailure() {
        IllegalStateException failure = new IllegalStateException("Service failed");
        IOException cleanup = new IOException("Cleanup failed");
        MemoryRequest request = this.request("GET", "/");
        request.resource(() -> {
            throw cleanup;
        });
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, response) -> {
            throw failure;
        });
        assertSame(failure, assertThrows(IllegalStateException.class, () -> handler.handle(request, new MemoryResponse())));
        assertArrayEquals(new Throwable[] { cleanup }, failure.getSuppressed());
    }

    @Test
    void cleanupClosesEveryResourceAndPreservesAllFailuresAfterASuccessfulResponse() throws Exception {
        Closeable first = mock(Closeable.class);
        Closeable second = mock(Closeable.class);
        Closeable last = mock(Closeable.class);
        IOException firstFailure = new IOException("First resource failed to close");
        IOException secondFailure = new IOException("Second resource failed to close");
        doThrow(firstFailure).when(first).close();
        doThrow(secondFailure).when(second).close();
        MemoryRequest request = this.request("GET", "/");
        request.resource(first);
        request.resource(second);
        request.resource(last);
        MemoryResponse response = new MemoryResponse();
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, output) -> ResultInfoUtils.buildSuccess("done"));

        assertSame(firstFailure, assertThrows(IOException.class, () -> handler.handle(request, response)));
        assertArrayEquals(new Throwable[] { secondFailure }, firstFailure.getSuppressed());
        assertEquals(200, response.getStatus());
        assertEquals("done", ((Map<?, ?>) response.json()).get("result"));
        assertTrue(request.isClosed());
        assertDoesNotThrow(request::close);
        verify(first).close();
        verify(second).close();
        verify(last).close();
    }

    @Test
    void binaryAndExplicitResultsRemainUnwrapped() throws Exception {
        byte[] bytes = { 0, 1, 2, (byte) 255 };
        ResultInfo binary = ResultInfoUtils.convertToResultInfo(bytes);
        assertSame(binary, ResultInfoUtils.convertToResultInfo(binary));
        assertFalse(binary.isJson());
        ResultWebHandler handler = new ResultWebHandler(this.beans(r -> UserIdentity.anonymous()), (r, output) -> binary);
        MemoryResponse response = this.handle(handler, "GET", "/");
        assertArrayEquals(bytes, response.bytes());
        assertEquals("4", response.getHeaders().get("Content-Length").get(0));

        ResultInfo json = ResultInfoUtils.convertToResultInfo(null);
        assertTrue(json.isJson());
        assertNull(json.getData());
        assertEquals(200, json.getStatus());
        assertEquals(Map.of("success", true, "code", 200, "message", "OK", "result", "ok"), ResultInfoUtils.buildSuccess("ok").getData());
    }
}
