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

/** Target and submitted parameters for a management operation. */
public class AdminInterceptorContext {
    private final ApiDefinition  definition;
    private final String         releaseId;
    private final Map<String, ?> parameters;
    private final Operation      operation;
    private final UserIdentity   identity;

    AdminInterceptorContext(ApiDefinition definition, String releaseId, Operation operation, UserIdentity identity, Map<String, ?> parameters) {
        this.definition = definition;
        this.releaseId = releaseId;
        this.parameters = parameters;
        this.operation = operation;
        this.identity = identity == null ? UserIdentity.anonymous(Map.of()) : identity;
    }

    /** Stored target, or the submitted definition when creating an API; null for collection operations. */
    public ApiDefinition definition() {
        return this.definition;
    }

    /** Selected history ID, or null when the operation does not target a publication snapshot. */
    public String releaseId() {
        return this.releaseId;
    }

    /** Decoded query parameters and submitted body fields. */
    public Map<String, ?> parameters() {
        return this.parameters;
    }

    public Operation operation() {
        return this.operation;
    }

    public UserIdentity identity() {
        return this.identity;
    }
}
