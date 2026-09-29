/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import net.hasor.core.Module;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.DatawayConfig;

/** Supplies per-scenario factory inputs while Boot creates its configuration beans. */
record TestBootstrap(DatawayConfig configuration, ApiDataAccessLayer storage, Module module) {
    static final ThreadLocal<TestBootstrap> CURRENT = new ThreadLocal<>();
}
