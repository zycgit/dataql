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
    private final String         releaseId;
    private final Map<String, ?> parameters;
    private final Operation      operation;
    private final UserIdentity   identity;
    private final ApiCallSource  source;

    ApiInterceptorContext(ApiDefinition definition, String releaseId, Operation operation, UserIdentity identity, ApiCallSource source, Map<String, ?> parameters) {
        this.definition = definition;
        this.releaseId = releaseId;
        this.parameters = parameters;
        this.operation = operation;
        this.identity = identity == null ? UserIdentity.anonymous(Map.of()) : identity;
        this.source = source;
    }

    public ApiDefinition definition() {
        return this.definition;
    }

    /** Identifies the publication snapshot; null for draft or editor execution. */
    public String releaseId() {
        return this.releaseId;
    }

    public Map<String, ?> parameters() {
        return this.parameters;
    }

    public Operation operation() {
        return this.operation;
    }

    public UserIdentity identity() {
        return this.identity;
    }

    /** The invocation source, independent of its operation and user identity. */
    public ApiCallSource source() {
        return this.source;
    }
}
