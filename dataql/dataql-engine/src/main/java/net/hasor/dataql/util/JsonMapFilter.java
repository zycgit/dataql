/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.util;
import com.fasterxml.jackson.annotation.JsonFilter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.jdk.MapProperty;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

/** Applies the default map policy; a writer can override it without rebuilding the mapper. */
@JsonFilter(JsonMapFilter.NAME)
final class JsonMapFilter extends SimpleBeanPropertyFilter {
    static final String NAME = "dataql.mapValues";

    @Override
    protected boolean include(PropertyWriter writer) {
        return !(writer instanceof MapProperty property) || property.getValue() != null;
    }
}
