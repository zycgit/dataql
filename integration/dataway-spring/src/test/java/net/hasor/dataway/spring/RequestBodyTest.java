/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.http.HttpServletRequest;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.web.body.UploadStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestBodyTest {
    @TempDir
    Path directory;

    @Test
    void configuredStorageReachesEveryHandlerBeforeIdentityResolution() throws Exception {
        byte[] content = { 0, -1, 13, 10, 2 };
        Path cache = this.directory.resolve("configured");
        Path unused = this.directory.resolve("changed-after-creation");
        AtomicReference<WebFile> observed = new AtomicReference<>();
        IllegalStateException stop = new IllegalStateException("Authentication rejected the upload");
        DatawayConfig config = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).uploadTempDirectory(cache).uploadMemoryThreshold(2).identityProvider(request -> {
            Map<String, Object> body = assertDoesNotThrow(request::readBody);
            WebFile file = (WebFile) body.get("file");
            observed.set(file);
            assertArrayEquals(content, assertDoesNotThrow(() -> file.openStream().readAllBytes()));
            List<Path> files = assertDoesNotThrow(() -> {
                try (var paths = Files.list(cache)) {
                    return paths.toList();
                }
            });
            assertEquals(1, files.size());
            assertArrayEquals(content, assertDoesNotThrow(() -> Files.readAllBytes(files.getFirst())));
            throw stop;
        });
        Dataway dataway = config.createDataway();
        config.uploadTempDirectory(unused).uploadMemoryThreshold(1024);
        var handlers = List.of(dataway.getApiHandler(), dataway.getAdminHandler(), dataway.getAdminUiHandler());
        for (var handler : handlers) {
            MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
            request.setMethod("POST");
            request.setRequestURI("/api/upload");
            request.setContentType("multipart/form-data; boundary=test");
            request.addFile(new MockMultipartFile("file", "test.bin", "application/octet-stream", content));
            DatawayController controller = new DatawayController("/api", handler);
            assertSame(stop, assertThrows(IllegalStateException.class, () -> controller.handle(request, new MockHttpServletResponse())));
            try (var paths = Files.list(cache)) {
                assertEquals(0, paths.count());
            }
            assertThrows(IOException.class, observed.get()::openStream);
        }
        assertFalse(Files.exists(unused));
    }

    @Test
    void jsonIsReadLazilyOnceThroughTheHostWrapper() throws Exception {
        MockHttpServletRequest nativeRequest = spy(new MockHttpServletRequest());
        nativeRequest.setContent("{\"name\":\"测试\"}".getBytes(StandardCharsets.UTF_8));
        var input = spy(nativeRequest.getInputStream());
        doReturn(input).when(nativeRequest).getInputStream();
        clearInvocations(nativeRequest);
        SpringWebRequest request = this.request(nativeRequest, "POST", "Application/JSON; Charset=\"UTF-8\"");
        verify(nativeRequest, never()).getInputStream();
        try (request) {
            Map<String, Object> body = request.readBody();
            assertEquals(Map.of("name", "测试"), body);
            assertSame(body, request.readBody());
            verify(nativeRequest, times(1)).getInputStream();
        }
        verify(input, never()).close();
    }

    private SpringWebRequest request(HttpServletRequest nativeRequest, String method, String contentType) {
        SpringWebRequest request = new SpringWebRequest(nativeRequest);
        request.setMethod(method);
        request.setHeaders(contentType == null ? Map.of() : Map.of("Content-Type", contentType));
        request.setQuery(nativeRequest.getQueryString());
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = { "text/plain", "application/json; charset=invalid-encoding" })
    void unsupportedHeadersAreRejectedBeforeOpeningTheStream(String contentType) throws Exception {
        HttpServletRequest nativeRequest = mock(HttpServletRequest.class);
        doThrow(new AssertionError("Body must not be opened")).when(nativeRequest).getInputStream();
        try (SpringWebRequest request = this.request(nativeRequest, "POST", contentType)) {
            DatawayException failure = assertThrows(DatawayException.class, request::readBody);
            assertEquals(415, failure.status());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "[]", "null", "{bad", "\"text\"" })
    void jsonMustContainAnObject(String json) throws Exception {
        MockHttpServletRequest nativeRequest = new MockHttpServletRequest();
        nativeRequest.setContent(json.getBytes(StandardCharsets.UTF_8));
        try (SpringWebRequest request = this.request(nativeRequest, "POST", "application/json")) {
            assertEquals(400, assertThrows(DatawayException.class, request::readBody).status());
        }
    }

    @Test
    void emptyBodyNeedsNoContentTypeButNonEmptyBodyDoes() throws Exception {
        MockHttpServletRequest nativeRequest = new MockHttpServletRequest();
        try (SpringWebRequest request = this.request(nativeRequest, "GET", null)) {
            assertEquals(Map.of(), request.readBody());
        }
        nativeRequest.setContent("data".getBytes(StandardCharsets.UTF_8));
        try (SpringWebRequest request = this.request(nativeRequest, "POST", null)) {
            assertEquals(415, assertThrows(DatawayException.class, request::readBody).status());
        }
    }

    @Test
    void jsonHonorsTheDeclaredCharset() throws Exception {
        MockHttpServletRequest nativeRequest = new MockHttpServletRequest();
        nativeRequest.setContent("{\"name\":\"café\"}".getBytes(StandardCharsets.ISO_8859_1));
        try (SpringWebRequest request = this.request(nativeRequest, "POST", "application/json; charset=ISO-8859-1")) {
            assertEquals(Map.of("name", "café"), request.readBody());
        }
    }

    @Test
    void parsedFormsReuseHostParametersAndExcludeQueryValues() throws Exception {
        MockHttpServletRequest nativeRequest = spy(new MockHttpServletRequest());
        nativeRequest.setQueryString("name=query&same=x&onlyQuery=1");
        nativeRequest.addParameter("name", "query", "body");
        nativeRequest.addParameter("same", "x", "x");
        nativeRequest.addParameter("onlyQuery", "1");
        nativeRequest.addParameter("tag", "a", "b");
        doThrow(new AssertionError("Host already parsed the form")).when(nativeRequest).getInputStream();
        try (SpringWebRequest request = this.request(nativeRequest, "POST", "application/x-www-form-urlencoded")) {
            assertEquals(Map.of("name", "body", "same", "x", "tag", List.of("a", "b")), request.readBody());
        }
    }

    @Test
    void rawFormsSupportRepeatedFieldsEmptyValuesAndCharset() throws Exception {
        MockHttpServletRequest nativeRequest = new MockHttpServletRequest();
        nativeRequest.setContent("name=caf%E9&tag=a&tag=b&empty=&bare&space=a+b".getBytes(StandardCharsets.US_ASCII));
        try (SpringWebRequest request = this.request(nativeRequest, "PUT", "application/x-www-form-urlencoded; charset=ISO-8859-1")) {
            assertEquals(Map.of("name", "café", "tag", List.of("a", "b"), "empty", "", "bare", "", "space", "a b"), request.readBody());
        }
    }

    @Test
    void malformedRawFormReturnsBadRequest() throws Exception {
        MockHttpServletRequest nativeRequest = new MockHttpServletRequest();
        nativeRequest.setContent("name=%zz".getBytes(StandardCharsets.US_ASCII));
        try (SpringWebRequest request = this.request(nativeRequest, "PUT", "application/x-www-form-urlencoded")) {
            assertEquals(400, assertThrows(DatawayException.class, request::readBody).status());
        }
    }

    @Test
    void multipartReusesHostFilesAndFieldsWithoutReopeningTheBody() throws Exception {
        MockMultipartHttpServletRequest nativeRequest = spy(new MockMultipartHttpServletRequest());
        nativeRequest.addFile(new MockMultipartFile("file", "first.bin", "application/octet-stream", new byte[] { 0, -1, 2 }));
        nativeRequest.addFile(new MockMultipartFile("file", "empty.bin", "application/octet-stream", new byte[0]));
        nativeRequest.setQueryString("title=query");
        nativeRequest.addParameter("title", "query", "form");
        doThrow(new AssertionError("Host already parsed multipart")).when(nativeRequest).getInputStream();
        WebFile first;
        try (SpringWebRequest request = this.request(nativeRequest, "POST", "multipart/form-data; boundary=host")) {
            request.setUploadStorage(new UploadStorage(this.directory, 1));
            Map<String, Object> body = request.readBody();
            assertSame(body, request.readBody());
            assertEquals("form", body.get("title"));
            List<?> files = assertInstanceOf(List.class, body.get("file"));
            first = assertInstanceOf(WebFile.class, files.getFirst());
            assertEquals("first.bin", first.getName());
            assertEquals(3, first.getSize());
            assertArrayEquals(new byte[] { 0, -1, 2 }, first.openStream().readAllBytes());
            assertEquals(0, assertInstanceOf(WebFile.class, files.get(1)).getSize());
            try (var paths = Files.list(this.directory)) {
                assertEquals(1, paths.count());
            }
        }
        try (var paths = Files.list(this.directory)) {
            assertEquals(0, paths.count());
        }
        assertThrows(IOException.class, first::openStream);
    }

    @Test
    void cleanupRunsAfterStreamingTheResponse() throws Exception {
        byte[] bytes = { 0, -1, 13, 10, 2 };
        MockHttpServletRequest request = this.multipartRequest();
        MockPart part = spy(new MockPart("file", "test.bin", bytes));
        request.addPart(part);
        MockHttpServletResponse response = new MockHttpServletResponse();
        doAnswer(call -> {
            assertArrayEquals(bytes, response.getContentAsByteArray());
            return null;
        }).when(part).delete();
        UploadRequestHandler handler = new UploadRequestHandler();
        new DatawayController("/api", handler).handle(request, response);
        assertArrayEquals(bytes, response.getContentAsByteArray());
        verify(part, times(1)).delete();
        assertThrows(IOException.class, handler.file::openStream);
    }

    private MockHttpServletRequest multipartRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload");
        request.setContentType("multipart/form-data; boundary=test");
        return request;
    }

    @Test
    void cleanupAlsoRunsOnHandlerAndResponseFailures() throws Exception {
        for (boolean failInHandler : new boolean[] { true, false }) {
            MockHttpServletRequest request = this.multipartRequest();
            MockPart part = spy(new MockPart("file", "test.bin", new byte[] { 1, 2 }));
            request.addPart(part);
            MockHttpServletResponse response = spy(new MockHttpServletResponse());
            UploadRequestHandler handler = new UploadRequestHandler();
            IOException failure = new IOException("failure");
            if (failInHandler) {
                handler.failure = failure;
            } else {
                doThrow(failure).when(response).getOutputStream();
            }
            DatawayController controller = new DatawayController("/api", handler);
            assertSame(failure, assertThrows(IOException.class, () -> controller.handle(request, response)));
            verify(part, times(1)).delete();
            assertThrows(IOException.class, handler.file::openStream);
        }
    }
}
