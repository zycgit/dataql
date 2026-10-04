/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;

/** Identifies where an API execution was initiated. */
public enum ApiCallSource {
    /** Direct invocation through ApiService or the query engine. */
    PROGRAMMATIC,
    /** Editor execution or saved-draft smoke testing. */
    DEBUG,
    /** Invocation of a published API from the console interface list. */
    UI,
    /** Invocation through the public business API entry. */
    HTTP
}
