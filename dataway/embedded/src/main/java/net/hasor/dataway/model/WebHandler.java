/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.List;

/** A single HTTP entry point, invoked only after the host has selected its route. */
@FunctionalInterface
public interface WebHandler {
    /** Failures propagate to the host; the handler does not write an error response. */
    void handle(WebRequest request, WebResponse response) throws Exception;

    /**
     * Paths relative to this entry. A trailing /* denotes a subtree; other paths are exact.
     * The host adds its prefix, validates mappings and translates them to native routes.
     * An empty list uses the entry's default paths when assembled by Dataway.
     */
    default List<String> paths() {
        return List.of();
    }
}
