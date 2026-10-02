/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.io.IOException;
import java.io.InputStream;

/** Opaque binary content. Conversions preserve its identity without opening or consuming it. */
public abstract class BinaryModel implements DataModel {
    /** Opens the content for its consumer, which must close the returned stream. */
    public abstract InputStream openStream() throws IOException;

    /** Returns the byte length, or -1 when unknown. */
    public long getSize() {
        return -1;
    }

    @Override
    public final BinaryModel asOri() {
        return this;
    }

    @Override
    public final BinaryModel unwrap() {
        return this;
    }

    @Override
    public final boolean isBinary() {
        return true;
    }
}
