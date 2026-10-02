/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps handler defaults separate from the options prepared for each query. */
public abstract class AbstractResultHandler implements ResultHandler {
    private final Map<String, Object> defaults;

    protected AbstractResultHandler(Map<String, ?> defaults) {
        this.defaults = this.copyOptions(defaults);
    }

    @Override
    public Map<String, Object> prepareOptions(Map<String, ?> options) {
        Map<String, Object> resolved = this.copyOptions(this.defaults);
        if (options != null) {
            resolved.putAll(this.copyOptions(options));
        }
        return resolved;
    }

    private Map<String, Object> copyOptions(Map<String, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, value) -> copy.put(name, this.copyValue(value)));
        }
        return copy;
    }

    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(key, this.copyValue(item)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(this.copyValue(item));
            }
            return copy;
        }
        return value;
    }
}
