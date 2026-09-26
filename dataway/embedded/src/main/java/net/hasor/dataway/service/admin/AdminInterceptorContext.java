/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.util.Map;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ApiDefinition;

/** Definition, parameters, operation and identity for API and administration interception. */
public class AdminInterceptorContext {
    private final ApiDefinition  definition;
    private final Map<String, ?> parameters;
    private final Operation      operation;
    private final UserIdentity   identity;

    AdminInterceptorContext(ApiDefinition definition, Operation operation, UserIdentity identity, Map<String, ?> parameters) {
        this.definition = definition;
        this.parameters = parameters;
        this.operation = operation;
        this.identity = identity == null ? UserIdentity.anonymous() : identity;
    }

    /** Null at HTTP entries before service lookup, and for collection or missing-target operations. */
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
