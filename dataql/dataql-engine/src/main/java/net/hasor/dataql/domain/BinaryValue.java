/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/** Explicit binary UDF value. Bytes are repeatable; a supplied stream can be opened only once. */
public class BinaryValue extends BinaryModel implements Closeable {
    private final byte[]      bytes;
    private final InputStream source;
    private       boolean     opened;
    private       boolean     closed;

    public BinaryValue(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Binary bytes must not be null");
        }
        this.bytes = bytes;
        this.source = null;
    }

    public BinaryValue(InputStream source) {
        if (source == null) {
            throw new IllegalArgumentException("Binary source must not be null");
        }
        this.bytes = null;
        this.source = source;
    }

    @Override
    public InputStream openStream() throws IOException {
        if (this.closed || (this.source != null && this.opened)) {
            throw new IOException("Binary source has already been consumed or closed");
        }
        this.opened = true;
        return this.bytes == null ? this.source : new ByteArrayInputStream(this.bytes);
    }

    @Override
    public long getSize() {
        return this.bytes == null ? -1 : this.bytes.length;
    }

    @Override
    public void close() throws IOException {
        if (!this.closed) {
            this.closed = true;
            if (this.source != null) {
                this.source.close();
            }
        }
    }
}
