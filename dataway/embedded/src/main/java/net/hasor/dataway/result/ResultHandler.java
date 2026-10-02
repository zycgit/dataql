/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;

/** Converts an execution outcome into an HTTP response. Implementations may be shared by queries. */
@FunctionalInterface
public interface ResultHandler {
    /** Resolves defaults and validates API options before script execution. */
    default Map<String, Object> prepareOptions(Map<String, ?> options) {
        return options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }

    ResultInfo handle(ResultContext context) throws Exception;
}
