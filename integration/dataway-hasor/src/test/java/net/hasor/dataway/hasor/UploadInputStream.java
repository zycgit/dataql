/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.io.ByteArrayInputStream;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

class UploadInputStream extends ServletInputStream {
    private final ByteArrayInputStream input;
    boolean closed;

    UploadInputStream(byte[] bytes) {
        this.input = new ByteArrayInputStream(bytes);
    }

    @Override
    public int read() {
        return this.input.read();
    }

    @Override
    public boolean isFinished() {
        return this.input.available() == 0;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
    }

    @Override
    public void close() {
        this.closed = true;
    }
}
