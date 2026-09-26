/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;

/** Script execution data created by DatawayQuery and supplied to API interceptors. */
public class ApiInterceptorContext {
    private final ApiDefinition  definition;
    private final Map<String, ?> parameters;
    private final Operation      operation;
    private final UserIdentity   identity;

    ApiInterceptorContext(ApiDefinition definition, Operation operation, UserIdentity identity, Map<String, ?> parameters) {
        this.definition = definition;
        this.parameters = parameters;
        this.operation = operation;
        this.identity = identity == null ? UserIdentity.anonymous() : identity;
    }

    public ApiDefinition getDefinition() {
        return this.definition;
    }

    public Map<String, ?> getParameters() {
        return this.parameters;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public UserIdentity getIdentity() {
        return this.identity;
    }
}
