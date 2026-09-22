/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.Map;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.DataMutation;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LazyDataAccessLayerTest {
    @Test
    void deferredAccessPreservesTheCustomMutationFactory() {
        var mutation = new DataMutation() {
        };
        var access = new ApiDataAccessLayer() {
            public java.util.List<Map<FieldDef, String>> listObjects(EntityType type, Map<FieldDef, String> conditions) {
                return java.util.List.of();
            }

            public void write(java.util.List<DataMutation> mutations) {
            }

            public DataMutation create() {
                return mutation;
            }
        };
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var deferred = new LazyDataAccessLayer(() -> {
            calls.incrementAndGet();
            return access;
        });
        assertSame(mutation, deferred.create());
        deferred.initialize();
        assertEquals(1, calls.get());
    }
}
