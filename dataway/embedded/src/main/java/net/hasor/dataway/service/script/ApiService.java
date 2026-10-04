/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;

/** Invokes published, enabled APIs without an HTTP request. */
public interface ApiService {
    /**
     * Invokes the published API identified by its method and path relative to the API entry.
     * Java calls do not require a user identity or run HTTP authorization checks.
     * API interceptors and the configured result handler also apply to this invocation.
     * HTTP request and response functions have no host context here.
     * The caller owns any stream or binary resource returned in the result.
     */
    ResultInfo invokeByPath(String method, String path, Map<String, ?> parameters) throws Exception;

    /**
     * Invokes the enabled publication of the specified API, using the same execution flow as invocation by path.
     * apiID identifies the API definition, not a release or history record.
     */
    ResultInfo invokeById(String apiID, Map<String, ?> parameters) throws Exception;
}
