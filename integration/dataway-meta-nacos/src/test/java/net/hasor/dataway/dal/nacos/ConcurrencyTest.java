/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.alibaba.nacos.api.exception.NacosException;
import net.hasor.dataway.dal.DataAccessException;
import net.hasor.dataway.dal.DataConflictException;
import net.hasor.dataway.dal.EntityType;
import org.junit.jupiter.api.Test;
import static net.hasor.dataway.dal.FieldDef.COMMENT;
import static net.hasor.dataway.dal.FieldDef.REVISION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConcurrencyTest {
    @Test
    void twoConcurrentWritersProduceOneWinnerAndOneExplicitConflict() throws Exception {
        NacosFixture fixture = new NacosFixture();
        fixture.access.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft"));
        CyclicBarrier bothRead = new CyclicBarrier(2);
        when(fixture.client.getConfig("metadata", "group", 2000)).thenAnswer(call -> {
            String snapshot = fixture.content();
            bothRead.await(5, TimeUnit.SECONDS);
            return snapshot;
        });
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> this.update(fixture.reconnect(), "first"));
            var second = pool.submit(() -> this.update(fixture.reconnect(), "second"));
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
        var row = NacosSnapshot.parse(fixture.content()).getRecords().get(EntityType.INFO).get("draft");
        assertEquals("2", row.get(REVISION));
        assertTrue(List.of("first", "second").contains(row.get(COMMENT)));
    }

    private boolean update(NacosDataAccessLayer access, String comment) {
        try {
            access.updateObject(EntityType.INFO, "draft", 1, Map.of(COMMENT, comment));
            return true;
        } catch (DataConflictException expected) {
            return false;
        }
    }

    @Test
    void readAndPublishFailuresPreserveCausesAndNeverBlindlyRetry() throws Exception {
        NacosFixture fixture = new NacosFixture();
        NacosException read = new NacosException(500, "read timeout");
        doThrow(read).when(fixture.client).getConfig("metadata", "group", 2000);
        assertSame(read, assertThrows(DataAccessException.class, () -> fixture.access.listObjects(EntityType.INFO, Map.of())).getCause());
        doReturn(fixture.content()).when(fixture.client).getConfig("metadata", "group", 2000);
        NacosException publish = new NacosException(500, "ambiguous write timeout");
        doThrow(publish).when(fixture.client).publishConfigCas(anyString(), anyString(), anyString(), anyString());
        assertSame(publish, assertThrows(DataAccessException.class, () -> fixture.access.createObject(EntityType.INFO, "draft", NacosFixture.route("GET", "/draft"))).getCause());
        verify(fixture.client, times(1)).publishConfigCas(anyString(), anyString(), anyString(), anyString());
        assertTrue(NacosSnapshot.parse(fixture.content()).getRecords().get(EntityType.INFO).isEmpty());
    }
}
