/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.web.body.UploadStorage;
import net.hasor.web.upload.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RequestBodyTest {
    @TempDir
    Path directory;

    @Test
    void multipartSupportsFieldsBinaryFilesRepeatedNamesAndCleanup() throws Exception {
        String content = "--upload\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n测试\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"file\"; filename=\"../one.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n\u0000binary\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"file\"; filename=\"empty.bin\"\r\n\r\n\r\n--upload--\r\n";
        UploadInputStream input = new UploadInputStream(content.getBytes(StandardCharsets.UTF_8));
        WebFile file;
        try (HasorWebRequest request = this.request(input, "multipart/form-data; boundary=\"upload\"", Map.of())) {
            request.setUploadStorage(new UploadStorage(this.directory, 4));
            Map<String, Object> body = request.readBody();
            assertSame(body, request.readBody());
            assertEquals("测试", body.get("title"));
            List<?> files = assertInstanceOf(List.class, body.get("file"));
            file = assertInstanceOf(WebFile.class, files.get(0));
            assertEquals("../one.bin", file.getName());
            assertEquals("application/octet-stream", file.getContentType());
            assertArrayEquals("\u0000binary".getBytes(StandardCharsets.UTF_8), file.openStream().readAllBytes());
            assertEquals(0, assertInstanceOf(WebFile.class, files.get(1)).getSize());
            try (var filesOnDisk = Files.list(this.directory)) {
                assertEquals(1, filesOnDisk.count());
            }
        }
        try (var filesOnDisk = Files.list(this.directory)) {
            assertEquals(0, filesOnDisk.count());
        }
        assertFalse(input.closed);
        assertThrows(IOException.class, file::openStream);
    }

    private HasorWebRequest request(UploadInputStream input, String contentType, Map<String, String[]> parameters) {
        HttpServletRequest nativeRequest = (HttpServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
            case "getInputStream" -> input;
            case "getContentType" -> contentType;
            case "getCharacterEncoding" -> "UTF-8";
            case "getContentLength" -> -1;
            case "getContentLengthLong" -> -1L;
            case "getParameterMap" -> parameters;
            case "getMethod" -> "POST";
            default -> null;
        });
        HasorWebRequest request = new HasorWebRequest(nativeRequest);
        request.setMethod("POST");
        request.setHeaders(Map.of("Content-Type", contentType));
        return request;
    }

    @Test
    void partialUploadsAndHostSizeLimitFailuresLeaveNoTemporaryFiles() throws Exception {
        for (boolean limited : new boolean[] { false, true }) {
            String content = "--upload\r\nContent-Disposition: form-data; name=\"file\"; filename=\"one\"\r\n\r\nfirst\r\n" + "--upload\r\nContent-Disposition: form-data; name=\"file\"; filename=\"two\"\r\n\r\ntruncated";
            HasorWebRequest request = this.request(new UploadInputStream(content.getBytes(StandardCharsets.UTF_8)), "multipart/form-data; boundary=upload", Map.of());
            request.setUploadStorage(new UploadStorage(this.directory, 2));
            if (limited) {
                FileUpload upload = new FileUpload();
                upload.setFileSizeMax(2);
                request.setFileUpload(upload);
            }
            assertThrows(Exception.class, () -> {
                try (request) {
                    request.readBody();
                }
            });
            try (var filesOnDisk = Files.list(this.directory)) {
                assertEquals(0, filesOnDisk.count());
            }
        }
    }

    @Test
    void parsedFormsDoNotReadTheConsumedHostStream() throws Exception {
        UploadInputStream input = new UploadInputStream(new byte[0]);
        try (HasorWebRequest request = this.request(input, "application/x-www-form-urlencoded", Map.of("name", new String[] { "query", "body" }, "tag", new String[] { "a", "b" }))) {
            request.setQuery("name=query");
            assertEquals(Map.of("name", "body", "tag", List.of("a", "b")), request.readBody());
        }
        assertFalse(input.closed);
    }
}
