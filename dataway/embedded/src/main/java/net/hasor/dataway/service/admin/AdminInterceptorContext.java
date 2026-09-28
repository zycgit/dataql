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
public record AdminInterceptorContext(ApiDefinition definition, Map<String, ?> parameters, Operation operation, UserIdentity identity) {
    AdminInterceptorContext(ApiDefinition definition, Operation operation, UserIdentity identity, Map<String, ?> parameters) {
        this(definition, parameters, operation, identity == null ? UserIdentity.anonymous(Map.of()) : identity);
    }

    /** Null at HTTP entries before service lookup, and for collection or missing-target operations. */
    @Override
    public ApiDefinition definition() {
        return this.definition;
    }
}
