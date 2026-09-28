/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Supplies shared dependencies while constructing services and handlers. */
public class BeanContainer {
    private final Map<Class<?>, List<Object>> beans = new LinkedHashMap<>();

    public BeanContainer() {
    }

    /** Replaces all objects registered under this type with the supplied object. */
    public <T> void setBean(Class<T> type, T bean) {
        List<Object> values = this.beans.computeIfAbsent(type, i -> new ArrayList<>());
        values.clear();
        values.add(bean);
    }

    public <T> void addBean(Class<T> type, T bean) {
        this.beans.computeIfAbsent(type, i -> new ArrayList<>()).add(bean);
    }

    /** Returns the single object registered under this type. */
    public <T> T getBean(Class<T> type) {
        List<Object> values = this.beans.getOrDefault(type, List.of());
        if (values.size() != 1) {
            throw new IllegalStateException("Expected one bean of type " + type.getName() + "; found " + values.size());
        }

        return type.cast(values.get(0));
    }

    /** Returns an immutable snapshot in registration order; absent types return an empty list. */
    public <T> List<T> getBeans(Class<T> type) {
        return this.beans.getOrDefault(type, List.of()).stream().map(type::cast).toList();
    }
}
