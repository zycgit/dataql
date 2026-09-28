/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** An upload valid until the request finishes. The client filename is metadata, never a storage path. */
public abstract class WebFile implements Closeable {
    private final List<InputStream> streams = new ArrayList<>();
    private       boolean           closed;

    public abstract String getName();

    public abstract String getContentType();

    public abstract long getSize();

    public final InputStream openStream() throws IOException {
        if (this.closed) {
            throw new IOException("Upload has been released");
        }
        InputStream stream = this.openContent();
        this.streams.add(stream);
        return stream;
    }

    protected abstract InputStream openContent() throws IOException;

    public final void writeTo(OutputStream output) throws IOException {
        try (InputStream input = this.openStream()) {
            input.transferTo(output);
        }
    }

    @Override
    public final void close() throws IOException {
        if (this.closed) {
            return;
        }

        this.closed = true;
        IOException failure = null;
        for (InputStream stream : this.streams) {
            try {
                stream.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }

        this.streams.clear();

        try {
            this.deleteContent();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    protected abstract void deleteContent() throws IOException;
}
