/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.spi;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostContext;

/** SPI factory for a UDF source type. */
public interface UdfSourceFactory {

    String getResourceName();

    UdfSource create(HostContext context);
}
