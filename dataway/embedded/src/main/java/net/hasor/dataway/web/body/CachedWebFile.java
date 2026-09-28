/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.hasor.dataway.model.WebFile;

/** Keeps small uploads in memory and spills the complete content when the threshold is exceeded. */
public class CachedWebFile extends WebFile {
    private final String name;
    private final String contentType;
    private       byte[] content;
    private       Path   path;
    private       long   size;

    CachedWebFile(String name, String contentType, InputStream input, Path directory, int threshold) throws IOException {
        this.name = name;
        this.contentType = contentType;
        ByteArrayOutputStream memory = new ByteArrayOutputStream(Math.min(threshold, 8192));

        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (this.size + count > threshold) {
                this.spill(input, memory, buffer, count, directory);
                return;
            }
            memory.write(buffer, 0, count);
            this.size += count;
        }

        this.content = memory.toByteArray();
    }

    private void spill(InputStream input, ByteArrayOutputStream memory, byte[] buffer, int count, Path directory) throws IOException {
        if (directory == null) {
            this.path = Files.createTempFile("dataway-upload-", ".tmp");
        } else {
            Files.createDirectories(directory);
            this.path = Files.createTempFile(directory, "dataway-upload-", ".tmp");
        }

        try (OutputStream output = Files.newOutputStream(this.path)) {
            memory.writeTo(output);
            output.write(buffer, 0, count);
            this.size += count + input.transferTo(output);
        } catch (IOException | RuntimeException | Error e) {
            try {
                Files.deleteIfExists(this.path);
            } catch (IOException cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    @Override
    protected InputStream openContent() throws IOException {
        if (this.path == null) {
            return new ByteArrayInputStream(this.content);
        } else {
            return Files.newInputStream(this.path);
        }
    }

    @Override
    protected void deleteContent() throws IOException {
        this.content = null;
        if (this.path != null) {
            Files.deleteIfExists(this.path);
        }
    }
}