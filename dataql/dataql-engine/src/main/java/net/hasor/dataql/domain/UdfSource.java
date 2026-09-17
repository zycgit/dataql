/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.util.Map;
import java.util.function.Supplier;
import net.hasor.dataql.kernel.Finder;

/**
 * UDF 源
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
@FunctionalInterface
public interface UdfSource {
    Supplier<Map<String, Udf>> getUdfResource(Finder finder);
}
