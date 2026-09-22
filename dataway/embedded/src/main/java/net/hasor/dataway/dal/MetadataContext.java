/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;

/** Host-owned configuration and resource lookup. Never creates arbitrary beans. */
public interface MetadataContext {
    String getProperty(String key, String defaultValue);

    /** Named lookup must fail if missing. Unnamed lookup fails if ambiguous; the host may supply a default adapter or return null. */
    <T> T getBean(String name, Class<T> type);

    ClassLoader getClassLoader();
}
