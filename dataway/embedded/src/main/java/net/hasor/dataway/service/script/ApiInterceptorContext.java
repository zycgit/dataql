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
public record ApiInterceptorContext(ApiDefinition definition, Map<String, ?> parameters, Operation operation, UserIdentity identity) {
    ApiInterceptorContext(ApiDefinition definition, Operation operation, UserIdentity identity, Map<String, ?> parameters) {
        this(definition, parameters, operation, identity == null ? UserIdentity.anonymous(Map.of()) : identity);
    }
}
