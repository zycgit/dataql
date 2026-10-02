/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import net.hasor.dataway.function.WebFile;

/** Shared upload cache policy; each cached file owns its memory or temporary file. */
public class UploadStorage {
    public static final int           DEFAULT_MEMORY_THRESHOLD = 50 * 1024;
    public static final UploadStorage DEFAULT                  = new UploadStorage(null, DEFAULT_MEMORY_THRESHOLD);
    private final       Path          tempDirectory;
    private final       int           memoryThreshold;

    public UploadStorage(Path tempDirectory, int memoryThreshold) {
        if (memoryThreshold < 0) {
            throw new IllegalArgumentException("uploadMemoryThreshold must not be negative");
        }

        this.tempDirectory = tempDirectory;
        this.memoryThreshold = memoryThreshold;
    }

    /** Reads the upload once. The caller retains ownership of the source stream. */
    public WebFile cache(String name, String contentType, InputStream input) throws IOException {
        return new CachedWebFile(name, contentType, input, this.tempDirectory, this.memoryThreshold);
    }
}