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
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.service.script.DatawayConfig;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdbcDataAccessLayerTest {
    private JdbcDataSource source() {
        return TestDatabase.create();
    }

    private ApiDefinition api(String id, String path, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("GET");
        definition.setPath(path);
        definition.setType(ApiScriptType.DATAQL);
        definition.setScript(script);
        definition.setDescription("描述");
        return definition;
    }

    @Test
    void persistenceRetainsDraftPublishedSnapshotAndHistoryAcrossServiceInstances() throws Exception {
        var source = source();
        var repository = new JdbcDataAccessLayer(source, "");

        var runtime = new DatawayConfig().resultStructure(false);
        var service = DatawayService.builder(runtime, repository).build();
        service.getBeanContainer().getAdminService().save(api("one", "/hello", "return 'published';"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        service.getBeanContainer().getAdminService().publish("one", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
        service.getBeanContainer().getAdminService().save(api("one", "/hello", "return 'draft';"), 2, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        var restarted = DatawayService.builder(runtime, new JdbcDataAccessLayer(source, "")).build();
        assertEquals("published", restarted.invokeApi("/hello", Map.of()));
        assertEquals("draft", restarted.debug("one", Map.of(), Operation.DEBUG, UserIdentity.anonymous(), Map.of(), null));
        assertEquals(1, restarted.getBeanContainer().getAdminService().getApiById("one", Operation.READ, UserIdentity.anonymous(), Map.of(), null).getHistory().size());
        assertEquals("描述", restarted.getBeanContainer().getAdminService().getApiById("one", Operation.READ, UserIdentity.anonymous(), Map.of(), null).getDraft().getDescription());
        restarted.getBeanContainer().getAdminService().disableApi("one", 3, Operation.DISABLE, UserIdentity.anonymous(), Map.of(), null);
        assertFalse(restarted.getBeanContainer().getAdminService().getApiById("one", Operation.READ, UserIdentity.anonymous(), Map.of(), null).isEnabled());
        restarted.getBeanContainer().getAdminService().deleteApi("one", 4, Operation.DELETE, UserIdentity.anonymous(), Map.of(), null);
        assertTrue(service.getBeanContainer().getAdminService().list(Operation.LIST, UserIdentity.anonymous(), Map.of(), null).isEmpty());
    }

    @Test
    void separateRepositoryInstancesEnforceAtomicPublication() throws Exception {
        var source = source();
        var first = new JdbcDataAccessLayer(source, "");

        var runtime = new DatawayConfig().resultStructure(false);
        var a = DatawayService.builder(runtime, first).build();
        var b = DatawayService.builder(runtime, new JdbcDataAccessLayer(source, "")).build();
        a.getBeanContainer().getAdminService().save(api("one", "/hello", "return 1;"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var x = pool.submit(() -> publish(start, a));
            var y = pool.submit(() -> publish(start, b));
            start.countDown();
            assertNotEquals(x.get(), y.get());
        }
        assertEquals(2, a.getBeanContainer().getAdminService().getApiById("one", Operation.READ, UserIdentity.anonymous(), Map.of(), null).getRevision());
        assertEquals(1, a.getBeanContainer().getAdminService().getApiById("one", Operation.READ, UserIdentity.anonymous(), Map.of(), null).getHistory().size());
    }

    private boolean publish(CountDownLatch start, DatawayService service) throws Exception {
        start.await();
        try {
            service.getBeanContainer().getAdminService().publish("one", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
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

        var service = DatawayService.builder(new DatawayConfig().resultStructure(false), repository).build();
        service.getBeanContainer().getAdminService().save(api("one", "/path", "return 1;"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        service.getBeanContainer().getAdminService().save(api("two", "/Path", "return 2;"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.getBeanContainer().getAdminService().save(api("three", "/path", "return 3;"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null)).status());
        service.getBeanContainer().getAdminService().save(api("one", "/path", "return 4;"), 1, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.getBeanContainer().getAdminService().deleteApi("one", 1, Operation.DELETE, UserIdentity.anonymous(), Map.of(), null)).status());
        assertEquals(2, service.getBeanContainer().getAdminService().list(Operation.LIST, UserIdentity.anonymous(), Map.of(), null).size());
        assertTrue(repository.getObject(EntityType.INFO, "' OR 1=1 --").isEmpty());
    }
}
