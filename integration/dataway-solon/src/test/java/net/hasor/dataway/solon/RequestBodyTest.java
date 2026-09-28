/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.web.body.UploadStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.noear.solon.Solon;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.core.handle.UploadedFile;
import static org.junit.jupiter.api.Assertions.*;

class RequestBodyTest {
    @TempDir
    Path directory;

    @Test
    void actualServerReusesMultipartParsedBeforeDataway() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Solon.start(RequestBodyTest.class, new String[] { "--server.port=" + port }, a -> {
            a.router().post("/upload", context -> {
                context.paramMap();
                context.fileMap();
                try (SolonWebRequest request = new SolonWebRequest(context)) {
                    request.setUploadStorage(new UploadStorage(this.directory, 1));
                    request.setHeaders(Map.of("Content-Type", context.contentType()));
                    request.setQuery(context.queryString());
                    Map<String, Object> body = request.readBody();
                    assertFalse(body.containsKey("trace"));
                    WebFile file = (WebFile) body.get("file");
                    try (var files = Files.list(this.directory)) {
                        assertEquals(1, files.count());
                    }
                    String result = JsonUtils.writeValueAsString(Map.of("name", body.get("name"), "tag", body.get("tag"), "filename", file.getName(), "content", new String(file.openStream().readAllBytes(), StandardCharsets.UTF_8)));
                    context.output(result);
                }
            });
        });
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            String multipart = "--upload\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\nbody\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"tag\"\r\n\r\na\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"tag\"\r\n\r\nb\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\nContent-Type: text/plain\r\n\r\nhello\r\n--upload--\r\n";
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/upload?trace=query")).timeout(Duration.ofSeconds(10)).header("Content-Type", "multipart/form-data; boundary=upload").POST(HttpRequest.BodyPublishers.ofString(multipart)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertEquals(Map.of("name", "body", "tag", List.of("a", "b"), "filename", "test.txt", "content", "hello"), JsonUtils.readValue(response.body(), Map.class));
        } finally {
            Solon.stopBlock();
        }
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void formUsesHostParametersWithoutReopeningTheBody() throws Exception {
        ContextEmpty context = new ContextEmpty();
        context.paramMap().add("name", "query");
        context.paramMap().add("name", "body");
        context.paramMap().add("tag", "a");
        context.paramMap().add("tag", "b");
        try (SolonWebRequest request = new SolonWebRequest(context)) {
            request.setQuery("name=query");
            request.setHeaders(Map.of("Content-Type", "application/x-www-form-urlencoded"));
            assertEquals(Map.of("name", "body", "tag", List.of("a", "b")), request.readBody());
        }
    }

    @Test
    void multipartUsesHostFilesAndDeletesThemOnRequestCompletion() throws Exception {
        AtomicInteger deleted = new AtomicInteger();
        ContextEmpty context = new ContextEmpty();
        byte[] bytes = { 0, -1, 1 };
        UploadedFile first = new UploadedFile(() -> deleted.incrementAndGet(), "application/octet-stream", bytes.length, new ByteArrayInputStream(bytes), "first.bin", "bin");
        UploadedFile empty = new UploadedFile(() -> deleted.incrementAndGet(), "application/octet-stream", 0, new ByteArrayInputStream(new byte[0]), "empty.bin", "bin");
        context.fileMap().add("files", first);
        context.fileMap().add("files", empty);
        context.paramMap().add("name", "query");
        context.paramMap().add("name", "form");
        WebFile file;
        SolonWebRequest request = new SolonWebRequest(context);
        try (request) {
            request.setUploadStorage(new UploadStorage(this.directory, 1));
            request.setQuery("name=query");
            request.setHeaders(Map.of("Content-Type", "multipart/form-data; boundary=host"));
            Map<String, Object> body = request.readBody();
            assertSame(body, request.readBody());
            assertEquals("form", body.get("name"));
            List<?> files = assertInstanceOf(List.class, body.get("files"));
            file = assertInstanceOf(WebFile.class, files.getFirst());
            assertEquals("first.bin", file.getName());
            assertEquals(bytes.length, file.getSize());
            assertArrayEquals(bytes, file.openStream().readAllBytes());
            assertArrayEquals(bytes, file.openStream().readAllBytes());
            assertEquals(0, assertInstanceOf(WebFile.class, files.get(1)).getSize());
            assertEquals(0, deleted.get());
        }
        assertEquals(2, deleted.get());
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
        request.close();
        assertEquals(2, deleted.get());
        assertThrows(IOException.class, file::openStream);
    }

    @Test
    void cleanupContinuesAfterOneFileFailsAndPreservesTheOriginalFailure() throws Exception {
        IOException cleanup = new IOException("delete failed");
        IOException original = new IOException("action failed");
        AtomicInteger deleted = new AtomicInteger();
        ContextEmpty context = new ContextEmpty();
        context.fileMap().add("file", new UploadedFile(() -> {
            throw cleanup;
        }, "text/plain", 1, new ByteArrayInputStream(new byte[] { 1 }), "first", ""));
        context.fileMap().add("file", new UploadedFile(() -> deleted.incrementAndGet(), "text/plain", 1, new ByteArrayInputStream(new byte[] { 2 }), "second", ""));
        IOException result = assertThrows(IOException.class, () -> {
            try (SolonWebRequest request = new SolonWebRequest(context)) {
                request.setHeaders(Map.of("Content-Type", "multipart/form-data; boundary=host"));
                request.readBody();
                throw original;
            }
        });
        assertSame(original, result);
        assertArrayEquals(new Throwable[] { cleanup }, result.getSuppressed());
        assertEquals(1, deleted.get());
    }

    @Test
    void failedCopyReleasesEveryHostFileIncludingUnreadFiles() throws Exception {
        AtomicInteger deleted = new AtomicInteger();
        ContextEmpty context = new ContextEmpty();
        InputStream broken = InputStream.nullInputStream();
        broken.close();
        context.fileMap().add("file", new UploadedFile(() -> deleted.incrementAndGet(), "text/plain", 3, new ByteArrayInputStream(new byte[] { 1, 2, 3 }), "first", ""));
        context.fileMap().add("file", new UploadedFile(() -> deleted.incrementAndGet(), "text/plain", 1, broken, "broken", ""));
        context.fileMap().add("file", new UploadedFile(() -> deleted.incrementAndGet(), "text/plain", 1, new ByteArrayInputStream(new byte[] { 4 }), "unread", ""));
        assertThrows(IOException.class, () -> {
            try (SolonWebRequest request = new SolonWebRequest(context)) {
                request.setUploadStorage(new UploadStorage(this.directory, 1));
                request.setHeaders(Map.of("Content-Type", "multipart/form-data; boundary=host"));
                request.readBody();
            }
        });
        assertEquals(3, deleted.get());
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
    }
}
