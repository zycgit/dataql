/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast;
import java.io.IOException;
import java.io.Writer;

/**
 * DataQL 代码格式化的输出 Writer
 */
public class FormatWriter extends Writer {
    private final Writer writer;

    public FormatWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void write(char[] cBuf, int off, int len) throws IOException {
        this.writer.write(cBuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }
}
