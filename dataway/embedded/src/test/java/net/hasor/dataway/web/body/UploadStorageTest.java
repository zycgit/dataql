/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataway.function.WebFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadStorageTest {
    @TempDir
    Path directory;

    @Test
    void conversionFunctionsReadUploadsWithoutReleasingTheRequestFile() throws Exception {
        byte[] content = "你好".getBytes(StandardCharsets.UTF_8);
        WebFile file = new UploadStorage(this.directory, 0).cache("text.txt", "text/plain", new ByteArrayInputStream(content));
        try (file) {
            Object result = new QueryManager(new HostConfiguration()).newBuilder().createQuery("""
                    import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;
                    return [convert.byteToHex(${file}), convert.byteToString(${file})];
                    """).execute(symbol -> Map.of("file", file)).getData().unwrap();
            assertEquals(List.of("E4BDA0E5A5BD", "你好"), result);
            this.assertFileCount(1);
            try (InputStream input = file.openStream()) {
                assertArrayEquals(content, input.readAllBytes());
            }
        }
        this.assertFileCount(0);
        assertThrows(IOException.class, file::openStream);
    }

    @ParameterizedTest
    @CsvSource({ "0,0,false", "4,4,false", "5,4,true", "1,0,true", "8192,8192,false", "20000,10000,true" })
    void uploadsRemainInMemoryUntilTheThresholdIsExceeded(int size, int threshold, boolean spilled) throws Exception {
        byte[] content = new byte[size];
        new Random(27).nextBytes(content);
        InputStream input = spy(new ByteArrayInputStream(content));
        WebFile file = new UploadStorage(this.directory, threshold).cache("../../client.bin", "application/octet-stream", input);
        try (file) {
            assertEquals("../../client.bin", file.getName());
            assertEquals("application/octet-stream", file.getContentType());
            assertEquals(size, file.getSize());
            this.assertFileCount(spilled ? 1 : 0);
            assertArrayEquals(content, file.openStream().readAllBytes());
            assertArrayEquals(content, file.openStream().readAllBytes());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            file.writeTo(output);
            assertArrayEquals(content, output.toByteArray());
            if (spilled) {
                try (var paths = Files.list(this.directory)) {
                    Path stored = paths.findFirst().orElseThrow();
                    assertTrue(stored.getFileName().toString().startsWith("dataway-upload-"));
                    assertArrayEquals(content, Files.readAllBytes(stored));
                }
            }
        }
        this.assertFileCount(0);
        verify(input, never()).close();
        assertThrows(IOException.class, file::openStream);
        file.close();
    }

    private void assertFileCount(long expected) throws IOException {
        try (var files = Files.list(this.directory)) {
            assertEquals(expected, files.count());
        }
    }

    @Test
    void spilledUploadsCreateTheConfiguredDirectoryAndCloseOutstandingStreams() throws Exception {
        Path nested = this.directory.resolve("nested/uploads");
        WebFile file = new UploadStorage(nested, 0).cache("file.bin", null, new ByteArrayInputStream(new byte[] { 0, -1, 2 }));
        InputStream first = file.openStream();
        InputStream second = file.openStream();
        assertEquals(0, first.read());
        assertEquals(0, second.read());
        file.close();
        assertThrows(IOException.class, first::read);
        assertThrows(IOException.class, second::read);
        assertThrows(IOException.class, file::openStream);
        try (var files = Files.list(nested)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void failedStreamClosesDoNotPreventOtherStreamsOrTheTemporaryFileFromBeingReleased() throws Exception {
        CachedWebFile file = spy((CachedWebFile) new UploadStorage(this.directory, 0).cache("file.bin", null, new ByteArrayInputStream(new byte[] { 1 })));
        InputStream first = mock(InputStream.class);
        InputStream second = mock(InputStream.class);
        InputStream third = mock(InputStream.class);
        IOException firstFailure = new IOException("First stream failed to close");
        IOException secondFailure = new IOException("Second stream failed to close");
        doThrow(firstFailure).when(first).close();
        doThrow(secondFailure).when(second).close();
        doReturn(first, second, third).when(file).openContent();
        assertSame(first, file.openStream());
        assertSame(second, file.openStream());
        assertSame(third, file.openStream());

        assertSame(firstFailure, assertThrows(IOException.class, file::close));
        assertArrayEquals(new Throwable[] { secondFailure }, firstFailure.getSuppressed());
        this.assertFileCount(0);
        assertThrows(IOException.class, file::openStream);
        assertDoesNotThrow(file::close);
        verify(first).close();
        verify(second).close();
        verify(third).close();
        verify(file).deleteContent();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deletionFailuresAreReportedWithoutLosingAnEarlierStreamFailure(boolean streamFails) throws Exception {
        CachedWebFile file = spy((CachedWebFile) new UploadStorage(this.directory, 0).cache("file.bin", null, new ByteArrayInputStream(new byte[] { 1 })));
        InputStream stream = mock(InputStream.class);
        IOException streamFailure = new IOException("Stream failed to close");
        IOException deletionFailure = new IOException("Temporary file cannot be deleted");
        if (streamFails) {
            doThrow(streamFailure).when(stream).close();
        }
        doReturn(stream).when(file).openContent();
        doThrow(deletionFailure).when(file).deleteContent();
        file.openStream();

        IOException failure = assertThrows(IOException.class, file::close);
        if (streamFails) {
            assertSame(streamFailure, failure);
            assertArrayEquals(new Throwable[] { deletionFailure }, failure.getSuppressed());
        } else {
            assertSame(deletionFailure, failure);
            assertEquals(0, failure.getSuppressed().length);
        }
        this.assertFileCount(1);
        assertThrows(IOException.class, file::openStream);
        assertDoesNotThrow(file::close);
        verify(stream).close();
        verify(file).deleteContent();
    }

    @Test
    void theSystemTemporaryDirectoryCanBeUsedWhenNoneIsConfigured() throws Exception {
        WebFile file = new UploadStorage(null, 0).cache("system.bin", "application/octet-stream", new ByteArrayInputStream(new byte[] { 0, -1 }));
        InputStream stream = file.openStream();
        try (file) {
            assertArrayEquals(new byte[] { 0, -1 }, stream.readAllBytes());
        }
        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, file::openStream);
    }

    @ParameterizedTest
    @ValueSource(strings = { "io", "runtime", "error" })
    void failedSpillDeletesThePartialFileAndPreservesTheFailure(String kind) throws Exception {
        Throwable failure = switch (kind) {
            case "io" -> new IOException("upload disconnected");
            case "runtime" -> new IllegalStateException("upload cancelled");
            default -> new AssertionError("source failed");
        };
        InputStream source = mock(InputStream.class);
        when(source.read(any(byte[].class))).thenAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            buffer[0] = 1;
            buffer[1] = 2;
            return 2;
        });
        when(source.transferTo(any(OutputStream.class))).thenThrow(failure);
        UploadStorage storage = new UploadStorage(this.directory, 1);
        assertSame(failure, assertThrows(Throwable.class, () -> storage.cache("broken.bin", null, source)));
        this.assertFileCount(0);
        verify(source, never()).close();
    }

    @Test
    void anUnreadableSourceDoesNotCreateATemporaryFile() throws Exception {
        InputStream input = mock(InputStream.class);
        IOException failure = new IOException("connection closed");
        when(input.read(any(byte[].class))).thenThrow(failure);
        assertSame(failure, assertThrows(IOException.class, () -> new UploadStorage(this.directory, 0).cache("broken", null, input)));
        this.assertFileCount(0);
        verify(input, never()).close();
    }

    @Test
    void anUnusableTemporaryDirectoryFailsWithoutOverwritingExistingFiles() throws Exception {
        Path occupied = this.directory.resolve("occupied");
        Files.writeString(occupied, "keep");
        UploadStorage storage = new UploadStorage(occupied, 0);
        assertThrows(IOException.class, () -> storage.cache("file", null, new ByteArrayInputStream(new byte[] { 1 })));
        assertEquals("keep", Files.readString(occupied));
        this.assertFileCount(1);
    }

    @Test
    void negativeMemoryThresholdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UploadStorage(this.directory, -1));
    }
}
