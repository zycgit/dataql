/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.WebResponse;

/** Operation, host identity and request data for published API and administration interception. */
public final class InterceptorContext {
    private final String         target;
    private final ApiDefinition  definition;
    private final Operation      operation;
    private final UserIdentity   identity;
    private final Map<String, ?> request;
    private final WebResponse    response;
    private final Map<String, ?> parameters;

    public InterceptorContext(String target, ApiDefinition definition, Operation operation, UserIdentity identity, Map<String, ?> request, WebResponse response, Map<String, ?> parameters) {
        this.target = target;
        this.definition = definition;
        this.operation = Objects.requireNonNull(operation);
        this.identity = identity == null ? UserIdentity.anonymous() : identity;
        this.request = Collections.unmodifiableMap(new LinkedHashMap<>(request));
        this.response = response;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    public Operation getOperation() {
        return this.operation;
    }

    /** Requested API path at the HTTP entry, API id for service calls, or null when not yet resolved. */
    public String getTarget() {
        return this.target;
    }

    /** Null at HTTP entries before service lookup, and for collection or missing-target operations. */
    public ApiDefinition getDefinition() {
        return this.definition;
    }

    public Map<String, ?> getRequest() {
        return this.request;
    }

    public WebResponse getResponse() {
        return this.response;
    }

    public UserIdentity getIdentity() {
        return this.identity;
    }

    public Map<String, ?> getParameters() {
        return this.parameters;
    }
}
