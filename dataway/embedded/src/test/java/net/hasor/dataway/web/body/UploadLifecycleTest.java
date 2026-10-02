/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.function.WebFile;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.MemoryRequest;
import net.hasor.dataway.service.config.MemoryResponse;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static net.hasor.dataway.dal.FieldDef.METHOD;
import static net.hasor.dataway.dal.FieldDef.SCRIPT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exercises the contract after a host adapter parses multipart, including request-owned files. */
class UploadLifecycleTest extends ServiceTestSupport {
    @TempDir
    Path directory;

    @BeforeEach
    void provideApiIdentity() {
        this.config.identityProvider(r -> UserIdentity.authenticated("caller", Map.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "success", "query-failure", "response-failure", "missing-api" })
    void formFieldsAndRepeatedFilesReachTheApiAndAreReleasedOnEveryExit(String outcome) throws Exception {
        List<WebFile> uploads = new ArrayList<>();
        MemoryRequest request = this.uploadRequest(uploads);
        if (!outcome.equals("missing-api")) {
            this.publishUpload();
        }
        IOException failure = new IOException("response disconnected");
        MemoryResponse response = new MemoryResponse();
        if (outcome.equals("response-failure")) {
            response.failWith(failure);
        }
        this.config.uploadTempDirectory(this.directory).uploadMemoryThreshold(1).resultHandler("raw").apiInterceptor((context, chain) -> {
            assertEquals(Operation.INVOKE, context.operation());
            assertEquals("form title", context.parameters().get("title"));
            assertEquals(List.of("one", "two"), context.parameters().get("tag"));
            assertEquals(uploads, context.parameters().get("file"));
            assertArrayEquals("file content".getBytes(StandardCharsets.UTF_8), uploads.get(0).openStream().readAllBytes());
            assertEquals(0, uploads.get(1).getSize());
            this.assertFileCount(1);
            if (outcome.equals("query-failure")) {
                throw new IOException("script failed");
            }
            return chain.proceed(context);
        });
        Dataway dataway = this.config.createDataway();
        if (outcome.equals("response-failure")) {
            assertSame(failure, assertThrows(IOException.class, () -> dataway.getApiHandler().handle(request, response)));
        } else if (outcome.equals("missing-api")) {
            assertEquals(404, assertThrows(DatawayException.class, () -> dataway.getApiHandler().handle(request, response)).status());
        } else {
            dataway.getApiHandler().handle(request, response);
            assertEquals(outcome.equals("query-failure") ? "script failed" : "form title", response.json());
        }
        assertTrue(request.isClosed());
        assertEquals(2, uploads.size());
        for (WebFile file : uploads) {
            assertThrows(IOException.class, file::openStream);
        }
        this.assertFileCount(0);
        verify(request, never()).getBody();
    }

    private MemoryRequest uploadRequest(List<WebFile> uploads) throws IOException {
        MemoryRequest request = spy(this.request("POST", "/upload"));
        request.setQuery("title=query");
        request.setHeaders(Map.of("Content-Type", "multipart/form-data; boundary=host"));
        doAnswer(invocation -> {
            uploads.add(request.upload("file content"));
            uploads.add(request.upload(""));
            return Map.of("title", "form title", "tag", List.of("one", "two"), "file", uploads);
        }).when(request).getMultipartBody(StandardCharsets.UTF_8);
        return request;
    }

    private void publishUpload() {
        Map<FieldDef, String> release = this.release(this.info("upload", "1", 1), "release", "1", 1);
        release.put(METHOD, "POST");
        release.put(SCRIPT, "return ${title};");
        this.publishRoute(release);
    }

    private void assertFileCount(long expected) throws IOException {
        try (var files = Files.list(this.directory)) {
            assertEquals(expected, files.count());
        }
    }

    @Test
    void anUploadCanBeStreamedBackBeforeItsTemporaryFileIsReleased() throws Exception {
        List<WebFile> uploads = new ArrayList<>();
        MemoryRequest request = this.uploadRequest(uploads);
        this.publishUpload();
        this.config.uploadTempDirectory(this.directory).uploadMemoryThreshold(1).apiInterceptor((context, chain) -> uploads.get(0).openStream());
        MemoryResponse response = new MemoryResponse();
        this.config.createDataway().getApiHandler().handle(request, response);
        assertArrayEquals("file content".getBytes(StandardCharsets.UTF_8), response.bytes());
        assertEquals(List.of("application/octet-stream"), response.getHeaders().get("Content-Type"));
        this.assertFileCount(0);
        assertThrows(IOException.class, uploads.get(0)::openStream);
    }

    @Test
    void aPartialMultipartFailureReleasesFilesAlreadyCachedByTheAdapter() throws Exception {
        List<WebFile> uploads = new ArrayList<>();
        MemoryRequest request = this.uploadRequest(uploads);
        IOException failure = new IOException("unexpected end of multipart stream");
        doAnswer(invocation -> {
            uploads.add(request.upload("complete first file"));
            throw failure;
        }).when(request).getMultipartBody(StandardCharsets.UTF_8);
        this.config.uploadTempDirectory(this.directory).uploadMemoryThreshold(1);
        Dataway dataway = this.config.createDataway();
        assertSame(failure, assertThrows(IOException.class, () -> dataway.getApiHandler().handle(request, new MemoryResponse())));
        this.assertFileCount(0);
        assertThrows(IOException.class, uploads.get(0)::openStream);
        verify(this.access, never()).listObjects(eq(EntityType.RELEASE), anyMap());
    }

    @Test
    void rejectedUploadsDoNotParseOrCreateTemporaryFiles() throws Exception {
        List<WebFile> uploads = new ArrayList<>();
        MemoryRequest request = this.uploadRequest(uploads);
        this.config.uploadTempDirectory(this.directory).uploadMemoryThreshold(1).authorizationCheck((identity, operation) -> false);
        Dataway dataway = this.config.createDataway();
        assertEquals(401, assertThrows(DatawayException.class, () -> dataway.getApiHandler().handle(request, new MemoryResponse())).status());
        verify(request, never()).getMultipartBody(any());
        verify(request, never()).getBody();
        assertTrue(uploads.isEmpty());
        this.assertFileCount(0);
    }
}
