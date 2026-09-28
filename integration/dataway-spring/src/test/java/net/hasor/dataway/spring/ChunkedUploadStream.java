/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.io.InputStream;

/** Simulates a transport which supplies small chunks and may fail mid-upload. */
class ChunkedUploadStream extends InputStream {
    private final byte[] bytes;
    private       int    position;
    int     failAfter = Integer.MAX_VALUE;
    boolean closed;
    final IOException failure = new IOException("Upload interrupted");

    ChunkedUploadStream(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int read() throws IOException {
        if (this.position >= this.failAfter) {
            throw this.failure;
        }
        if (this.position == this.bytes.length) {
            return -1;
        }
        return this.bytes[this.position++] & 0xff;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        if (this.position >= this.failAfter) {
            throw this.failure;
        }
        if (this.position == this.bytes.length) {
            return -1;
        }
        int count = Math.min(Math.min(3, length), this.bytes.length - this.position);
        System.arraycopy(this.bytes, this.position, target, offset, count);
        this.position += count;
        return count;
    }

    @Override
    public void close() {
        this.closed = true;
    }
}
