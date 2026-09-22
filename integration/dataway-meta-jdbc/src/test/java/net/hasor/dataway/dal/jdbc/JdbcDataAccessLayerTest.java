/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import net.hasor.dataway.TestDatabase;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.FxRuntime;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdbcDataAccessLayerTest {
    private JdbcDataSource source() {
        return TestDatabase.create();
    }

    private ApiDefinition api(String id, String path, String script) {
        return new ApiDefinition(id, "GET", path, ScriptType.DATAQL, script, "描述");
    }

    @Test
    void persistenceRetainsDraftPublishedSnapshotAndHistoryAcrossServiceInstances() throws Exception {
        var source = source();
        var repository = new JdbcDataAccessLayer(source, "");

        var runtime = FxRuntime.builder().build();
        var service = DatawayService.builder(runtime, repository).build();
        service.save(api("one", "/hello", "return 'published';"), 0, CallContext.LOCAL);
        service.publish("one", 1, CallContext.LOCAL);
        service.save(api("one", "/hello", "return 'draft';"), 2, CallContext.LOCAL);
        var restarted = DatawayService.builder(runtime, new JdbcDataAccessLayer(source, "")).build();
        assertEquals("published", restarted.invokeApi("/hello", Map.of()));
        assertEquals("draft", restarted.debug("one", Map.of(), CallContext.LOCAL));
        assertEquals(1, restarted.getApiById("one", CallContext.LOCAL).history().size());
        assertEquals("描述", restarted.getApiById("one", CallContext.LOCAL).draft().description());
        restarted.disableApi("one", 3, CallContext.LOCAL);
        assertFalse(restarted.getApiById("one", CallContext.LOCAL).enabled());
        restarted.deleteApi("one", 4, CallContext.LOCAL);
        assertTrue(service.list(CallContext.LOCAL).isEmpty());
    }

    @Test
    void separateRepositoryInstancesEnforceAtomicPublication() throws Exception {
        var source = source();
        var first = new JdbcDataAccessLayer(source, "");

        var runtime = FxRuntime.builder().build();
        var a = DatawayService.builder(runtime, first).build();
        var b = DatawayService.builder(runtime, new JdbcDataAccessLayer(source, "")).build();
        a.save(api("one", "/hello", "return 1;"), 0, CallContext.LOCAL);
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var x = pool.submit(() -> publish(start, a));
            var y = pool.submit(() -> publish(start, b));
            start.countDown();
            assertNotEquals(x.get(), y.get());
        }
        assertEquals(2, a.getApiById("one", CallContext.LOCAL).revision());
        assertEquals(1, a.getApiById("one", CallContext.LOCAL).history().size());
    }

    private boolean publish(CountDownLatch start, DatawayService service) throws Exception {
        start.await();
        try {
            service.publish("one", 1, CallContext.LOCAL);
            return true;
        } catch (DatawayException e) {
            assertEquals(409, e.status());
            return false;
        }
    }

    @Test
    void uniqueRoutesAreCaseSensitiveAndStaleDeletesCannotRemoveChanges() {
        var source = source();
        var repository = new JdbcDataAccessLayer(source, "");

        var service = DatawayService.builder(FxRuntime.builder().build(), repository).build();
        service.save(api("one", "/path", "return 1;"), 0, CallContext.LOCAL);
        service.save(api("two", "/Path", "return 2;"), 0, CallContext.LOCAL);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.save(api("three", "/path", "return 3;"), 0, CallContext.LOCAL)).status());
        service.save(api("one", "/path", "return 4;"), 1, CallContext.LOCAL);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.deleteApi("one", 1, CallContext.LOCAL)).status());
        assertEquals(2, service.list(CallContext.LOCAL).size());
        assertTrue(repository.getObject(net.hasor.dataway.dal.EntityType.INFO, "' OR 1=1 --").isEmpty());
    }
}
