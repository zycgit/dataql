/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.web.body.UploadStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UploadStorageTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 7, 8, 9, 50_000 })
    void thresholdSpillsTheCompleteContentAndCleansTheFile(int length) throws Exception {
        Path cache = this.directory.resolve("nested/uploads");
        byte[] content = new byte[length];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        ChunkedUploadStream source = new ChunkedUploadStream(content);
        UploadStorage storage = new UploadStorage(cache, 8);
        try (WebFile file = storage.cache("../../test.bin", "application/octet-stream", source)) {
            assertEquals(content.length, file.getSize());
            assertEquals("../../test.bin", file.getName());
            assertEquals("application/octet-stream", file.getContentType());
            assertArrayEquals(content, file.openStream().readAllBytes());
            assertArrayEquals(content, file.openStream().readAllBytes());
            if (length > 8) {
                List<Path> paths = this.files(cache);
                assertEquals(1, paths.size());
                assertArrayEquals(content, Files.readAllBytes(paths.getFirst()));
            } else {
                assertFalse(Files.exists(cache));
            }
        }
        assertFalse(source.closed);
        assertEquals(List.of(), this.files(cache));
    }

    private List<Path> files(Path path) throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        try (var files = Files.list(path)) {
            return files.toList();
        }
    }

    @Test
    void zeroThresholdSpillsNonEmptyFilesOnly() throws Exception {
        UploadStorage storage = new UploadStorage(this.directory, 0);
        try (WebFile empty = storage.cache("empty", null, new ByteArrayInputStream(new byte[0])); WebFile nonEmpty = storage.cache("one", null, new ByteArrayInputStream(new byte[] { 1 }))) {
            assertEquals(0, empty.getSize());
            assertEquals(1, nonEmpty.getSize());
            assertEquals(1, this.files(this.directory).size());
        }
        assertEquals(List.of(), this.files(this.directory));
    }

    @Test
    void interruptedUploadDeletesItsPartialDiskCache() throws Exception {
        ChunkedUploadStream input = new ChunkedUploadStream(new byte[100]);
        input.failAfter = 12;
        UploadStorage storage = new UploadStorage(this.directory, 8);
        assertSame(input.failure, assertThrows(IOException.class, () -> storage.cache("broken", null, input)));
        assertEquals(List.of(), this.files(this.directory));
        assertFalse(input.closed);
    }

    @Test
    void invalidCacheDirectoryFailsOnlyWhenSpillingAndPreservesExistingFiles() throws Exception {
        Path occupied = this.directory.resolve("existing");
        Files.writeString(occupied, "keep");
        UploadStorage storage = new UploadStorage(occupied, 2);
        try (WebFile file = storage.cache("small", null, new ByteArrayInputStream(new byte[] { 1, 2 }))) {
            assertEquals(2, file.getSize());
        }
        assertThrows(IOException.class, () -> storage.cache("large", null, new ByteArrayInputStream(new byte[] { 1, 2, 3 })));
        assertEquals("keep", Files.readString(occupied));
        assertEquals(List.of(occupied), this.files(this.directory));
    }

    @Test
    void configurationUsesTheHasorDefaultThresholdAndRejectsNegativeThresholds() {
        assertEquals(50 * 1024, new DatawayConfig().getUploadMemoryThreshold());
        DatawayConfig config = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer()).uploadMemoryThreshold(-1);
        assertThrows(IllegalArgumentException.class, config::createDataway);
    }
}
