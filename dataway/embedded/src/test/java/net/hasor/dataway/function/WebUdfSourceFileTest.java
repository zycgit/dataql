/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataway.web.body.UploadStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebUdfSourceFileTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(ints = { 0, 65536 })
    void scriptsReadCompleteMemoryAndDiskUploadsWithoutReleasingThem(int threshold) throws Exception {
        byte[] content = "abc".repeat(4096).getBytes(StandardCharsets.UTF_8);
        try (WebFile file = new UploadStorage(this.directory, threshold).cache("../../data.txt", "text/plain", new ByteArrayInputStream(content))) {
            Map<?, ?> info = this.inspect(file);
            assertEquals("../../data.txt", info.get("name"));
            assertEquals(content.length, ((Number) info.get("size")).longValue());
            assertEquals("text/plain", info.get("contentType"));
            assertEquals("7834ab64afb43e3f7a58712a73251d9258453c373cccc3c6bd8154ab767fc06a", info.get("sha256"));
            assertArrayEquals(content, file.openStream().readAllBytes());
            assertEquals(info, this.inspect(file));
            try (var files = Files.list(this.directory)) {
                assertEquals(threshold == 0 ? 1 : 0, files.count());
            }
        }
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
    }

    private Map<?, ?> inspect(WebFile file) throws Exception {
        String script = "import '" + WebUdfSource.class.getName() + "' as web; return web.uploadFileInfo(${file});";
        Query query = new QueryManager(new HostConfiguration()).newBuilder().createQuery(script);
        return assertInstanceOf(Map.class, query.execute(Map.of("file", file)).getData().unwrap());
    }

    @Test
    void emptyUploadsCanOmitTheirContentType() throws Exception {
        try (WebFile file = new UploadStorage(this.directory, 1024).cache("empty.bin", null, new ByteArrayInputStream(new byte[0]))) {
            Map<?, ?> info = this.inspect(file);
            assertEquals("empty.bin", info.get("name"));
            assertEquals(0, ((Number) info.get("size")).longValue());
            assertNull(info.get("contentType"));
            assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", info.get("sha256"));
        }
    }

    @Test
    void failedReadsCloseTheStreamAndLeaveFileCleanupToTheRequest() throws Exception {
        WebFile file = mock(WebFile.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        InputStream stream = mock(InputStream.class);
        IOException failure = new IOException("Upload read failed");
        doReturn(stream).when(file).openContent();
        when(stream.read(any(byte[].class))).thenThrow(failure);

        try (file) {
            assertSame(failure, assertThrows(IOException.class, () -> new WebUdfSource().uploadFileInfo(file)));
            verify(stream).close();
            verify(file, never()).deleteContent();
        }
        verify(file).deleteContent();
    }
}
