/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.web.body;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;

/** Reads one body format without taking ownership of the host request stream. */
public interface BodyReader {
    Map<String, Object> read(WebRequest request, Charset charset) throws IOException;
}