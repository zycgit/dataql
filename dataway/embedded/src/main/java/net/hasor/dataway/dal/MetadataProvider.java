/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;

/** A stateless JDK SPI provider. Host resources arrive through create, not field injection. */
public interface MetadataProvider {
    String getName();

    ApiDataAccessLayer create(MetadataContext context);
}
