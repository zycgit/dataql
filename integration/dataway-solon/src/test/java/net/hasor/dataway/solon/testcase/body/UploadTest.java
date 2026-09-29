/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.body;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.solon.testcase.H2Database;
import net.hasor.dataway.solon.testcase.HttpClient;
import net.hasor.dataway.solon.testcase.TestApplication;
import net.hasor.dataway.solon.testcase.TestSettings;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UploadTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void diskBackedUploadsAreReleasedAfterSuccessfulOrFailedExecution(boolean fail) throws Throwable {
        List<WebFile> uploads = new CopyOnWriteArrayList<>();
        AtomicBoolean sawTemporaryFile = new AtomicBoolean();
        var config = TestSettings.configuration().uploadTempDirectory(this.directory).uploadMemoryThreshold(3).apiInterceptor((context, chain) -> {
            assertEquals("form", context.parameters().get("name"));
            assertEquals(List.of("one", "two"), context.parameters().get("tag"));
            WebFile file = (WebFile) context.parameters().get("file");
            uploads.add(file);
            assertEquals("upload.txt", file.getName());
            assertEquals("text/plain; charset=utf-8", file.getContentType());
            assertEquals(12, file.getSize());
            try (var input = file.openStream(); var files = Files.list(this.directory)) {
                assertEquals("upload bytes", new String(input.readAllBytes(), StandardCharsets.UTF_8));
                sawTemporaryFile.set(files.count() == 1);
            }
            if (fail) {
                throw new IOException("upload execution failed");
            }
            return chain.proceed(context);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/upload", "return ${name};");
            assertEquals(200, client.login("api").status);
            var multipart = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("name", "form").addFormDataPart("tag", "one").addFormDataPart("tag", "two").addFormDataPart("file", "upload.txt", RequestBody.create("upload bytes", MediaType.get("text/plain"))).build();
            var response = client.send("POST", "/api/upload?name=query", multipart);
            assertEquals(200, response.status, response.text());
            assertTrue(response.text().contains(fail ? "upload execution failed" : "form"), response.text());
            assertTrue(sawTemporaryFile.get(), response.text());
            assertEquals(1, uploads.size());
            this.assertReleased(uploads);
        }
    }

    private void assertReleased(List<WebFile> uploads) {
        // Socket EOF may precede the server's final cleanup by a few instructions.
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            while (true) {
                try (var files = Files.list(this.directory)) {
                    if (files.findAny().isEmpty()) {
                        break;
                    }
                }
                Thread.sleep(10);
            }
        });
        for (WebFile file : uploads) {
            assertThrows(IOException.class, file::openStream);
        }
    }

    @Test
    void repeatedSmallAndEmptyFilesStayInMemoryAndKeepBinaryContents() throws Throwable {
        List<WebFile> uploads = new CopyOnWriteArrayList<>();
        var config = TestSettings.configuration().uploadTempDirectory(this.directory).uploadMemoryThreshold(1024).apiInterceptor((context, chain) -> {
            List<?> values = (List<?>) context.parameters().get("files");
            for (Object value : values) {
                uploads.add((WebFile) value);
            }
            assertEquals(2, uploads.size());
            assertEquals("data.bin", uploads.get(0).getName());
            assertArrayEquals(new byte[] { 0, 1, -1 }, uploads.get(0).openStream().readAllBytes());
            assertEquals("empty.txt", uploads.get(1).getName());
            assertEquals(0, uploads.get(1).getSize());
            assertEquals(-1, uploads.get(1).openStream().read());
            try (var files = Files.list(this.directory)) {
                assertEquals(0, files.count());
            }
            return chain.proceed(context);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/upload", "return ${name};");
            assertEquals(200, client.login("api").status);
            var multipart = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("name", "上传表单").addFormDataPart("files", "data.bin", RequestBody.create(new byte[] { 0, 1, -1 }, MediaType.get("application/octet-stream"))).addFormDataPart("files", "empty.txt", RequestBody.create(new byte[0], MediaType.get("text/plain"))).build();
            var response = client.send("POST", "/api/upload", multipart);
            assertEquals(200, response.status, response.text());
            assertEquals("上传表单", JsonUtils.readValue(response.text(), String.class));
            assertEquals(2, uploads.size());
            this.assertReleased(uploads);
        }
    }

    @Test
    void unauthenticatedUploadsCannotExecuteTheApiOrRetainCacheFiles() throws Throwable {
        AtomicBoolean invoked = new AtomicBoolean();
        var config = TestSettings.configuration().uploadTempDirectory(this.directory).uploadMemoryThreshold(1).apiInterceptor((context, chain) -> {
            invoked.set(true);
            return chain.proceed(context);
        });
        try (H2Database database = new H2Database(); TestApplication app = new TestApplication(config, database.access, TestSettings.enabled()); HttpClient client = new HttpClient(app.baseUrl())) {
            database.publish(app.dataway(), "POST", "/upload", "return 1;");
            var multipart = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", "private.txt", RequestBody.create("private", MediaType.get("text/plain"))).build();
            assertEquals(401, client.send("POST", "/api/upload", multipart).status);
            assertFalse(invoked.get());
            this.assertReleased(List.of());
        }
    }
}
