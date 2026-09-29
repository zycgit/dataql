/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {
    @Test
    void concurrentUpdatesThroughTheSameAccessLayerHaveExactlyOneWinner() throws Exception {
        try (JdbcFixture fixture = new JdbcFixture()) {
            fixture.access.createObject(EntityType.INFO, "draft", JdbcFixture.info("GET", "/draft"));
            CyclicBarrier ready = new CyclicBarrier(2);
            var pool = Executors.newFixedThreadPool(2);
            try {
                var first = pool.submit(() -> this.update(fixture.access, ready, "first"));
                var second = pool.submit(() -> this.update(fixture.access, ready, "second"));
                assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            } finally {
                pool.shutdownNow();
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
            }
            var row = fixture.access.getObject(EntityType.INFO, "draft").orElseThrow();
            assertEquals("2", row.get(FieldDef.REVISION));
            assertTrue(row.get(FieldDef.COMMENT).equals("first") || row.get(FieldDef.COMMENT).equals("second"));
        }
    }

    private boolean update(JdbcDataAccessLayer access, CyclicBarrier ready, String comment) throws Exception {
        ready.await(5, TimeUnit.SECONDS);
        try {
            access.updateObject(EntityType.INFO, "draft", 1, Map.of(FieldDef.COMMENT, comment));
            return true;
        } catch (DataConflictException expected) {
            return false;
        }
    }
}
