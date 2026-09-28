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
import net.hasor.dataway.TestWebRequest;
import net.hasor.dataway.TestWebResponse;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayException;
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
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("描述");
        return definition;
    }

    @Test
    void persistenceRetainsDraftPublishedSnapshotAndHistoryAcrossCoreInstances() throws Exception {
        var source = source();
        var repository = new JdbcDataAccessLayer(source, "");

        var runtime = new DatawayConfig().resultStructure(false);
        var service = new Dataway(runtime.dataAccessLayer(repository));
        service.getAdminService().save(api("one", "/hello", "return 'published';"), 0);
        assertEquals(1, service.getAdminService().getVersionById("one"));
        service.getAdminService().publish("one", service.getAdminService().getVersionById("one"));
        assertEquals(2, service.getAdminService().getVersionById("one"));
        service.getAdminService().save(api("one", "/hello", "return 'draft';"), service.getAdminService().getVersionById("one"));
        var restarted = new Dataway(runtime.dataAccessLayer(new JdbcDataAccessLayer(source, "")));
        var restored = restarted.getAdminService().getApiById("one");
        assertEquals(3, restarted.getAdminService().getVersionById("one"));
        var published = new TestWebResponse();
        restarted.getApiHandler().handle(new TestWebRequest("GET", "/hello", Map.of()), published);
        assertEquals("published", published.getResult());
        var draft = new TestWebResponse();
        restarted.getAdminHandler().handle(new TestWebRequest("POST", "/smoke", Map.of("id", "one", "version", restored.getRevision())), draft);
        assertEquals("draft", draft.getResult());
        assertEquals(1, restarted.getAdminService().getHistoryByApi("one").size());
        assertEquals("描述", restarted.getAdminService().getDraftByApi("one").getDescription());
        restarted.getAdminService().disableApi("one", restarted.getAdminService().getVersionById("one"));
        assertFalse(restarted.getAdminService().getApiById("one").isEnabled());
        assertEquals(4, restarted.getAdminService().getVersionById("one"));
        restarted.getAdminService().deleteApi("one", restarted.getAdminService().getVersionById("one"));
        assertTrue(service.getAdminService().list().isEmpty());
        assertEquals(404, assertThrows(DatawayException.class, () -> restarted.getAdminService().getVersionById("one")).status());
    }

    @Test
    void separateQueriesPreserveDraftsPublicationSnapshotsAndStateFlags() {
        var service = new DatawayConfig().dataAccessLayer(new JdbcDataAccessLayer(this.source(), "")).createDataway().getAdminService();
        var created = service.save(this.api("one", "/one", "return 'original';"), 0);
        assertEquals("one", created.getApiID());
        assertEquals(1, created.getRevision());
        assertFalse(created.isPublished());
        assertFalse(created.isEnabled());
        assertTrue(created.isHasDraft());
        assertNull(service.list().getFirst().getScript());
        assertEquals("/one", service.list().getFirst().getPath());
        assertEquals("return 'original';", service.getDraftByApi("one").getScript());
        assertNull(service.getReleaseByApi("one"));
        assertTrue(service.getHistoryByApi("one").isEmpty());

        var published = service.publish("one", 1);
        assertEquals("one", published.getApiID());
        assertTrue(published.isPublished());
        assertTrue(published.isEnabled());
        assertFalse(published.isHasDraft());
        var first = service.getReleaseByApi("one");
        assertEquals(first, service.getHistoryById(first.getId()));
        assertEquals(first, service.getReleaseById(first.getId()));
        assertEquals(first, service.getHistoryByApi("one").getFirst());

        var changed = service.save(this.api("one", "/one", "return 'changed';"), 2);
        assertEquals("one", changed.getApiID());
        assertTrue(changed.isHasDraft());
        assertTrue(changed.isPublished());
        assertTrue(changed.isEnabled());
        assertEquals("return 'changed';", service.getDraftByApi("one").getScript());
        assertEquals("return 'original';", service.getReleaseByApi("one").getDefinition().getScript());
        var disabled = service.disableApi("one", 3);
        assertEquals("one", disabled.getApiID());
        assertTrue(disabled.isPublished());
        assertFalse(disabled.isEnabled());
        assertTrue(disabled.isHasDraft());
        assertEquals(first, service.getReleaseByApi("one"));
        assertEquals("one", service.getApiById("one").getApiID());
        assertEquals(disabled, service.getApiById("one"));

        service.publish("one", 4);
        var history = service.getHistoryByApi("one");
        assertEquals(2, history.size());
        assertEquals(1, history.get(0).getNumber());
        assertEquals(2, history.get(1).getNumber());
        assertEquals(first, history.get(0));
        assertEquals(history.get(1), service.getReleaseByApi("one"));
        assertEquals("return 'changed';", history.get(1).getDefinition().getScript());
        assertFalse(service.getApiById("one").isHasDraft());
        service.deleteApi("one", 5);
        assertEquals(404, assertThrows(DatawayException.class, () -> service.getHistoryById(first.getId())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.getReleaseById(first.getId())).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.getReleaseByApi("one")).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.getHistoryByApi("one")).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> service.getDraftByApi("one")).status());
    }

    @Test
    void separateRepositoryInstancesEnforceAtomicPublication() throws Exception {
        var source = source();
        var first = new JdbcDataAccessLayer(source, "");

        var runtime = new DatawayConfig().resultStructure(false);
        var a = new Dataway(runtime.dataAccessLayer(first));
        var b = new Dataway(runtime.dataAccessLayer(new JdbcDataAccessLayer(source, "")));
        a.getAdminService().save(api("one", "/hello", "return 1;"), 0);
        try (var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var x = pool.submit(() -> publish(start, a));
            var y = pool.submit(() -> publish(start, b));
            start.countDown();
            assertNotEquals(x.get(), y.get());
        }
        assertEquals(2, a.getAdminService().getApiById("one").getRevision());
        assertEquals(1, a.getAdminService().getHistoryByApi("one").size());
    }

    private boolean publish(CountDownLatch start, Dataway service) throws Exception {
        start.await();
        try {
            service.getAdminService().publish("one", 1);
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

        var service = new Dataway(new DatawayConfig().resultStructure(false).dataAccessLayer(repository));
        service.getAdminService().save(api("one", "/path", "return 1;"), 0);
        service.getAdminService().save(api("two", "/Path", "return 2;"), 0);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.getAdminService().save(api("three", "/path", "return 3;"), 0)).status());
        service.getAdminService().save(api("one", "/path", "return 4;"), 1);
        assertEquals(409, assertThrows(DatawayException.class, () -> service.getAdminService().deleteApi("one", 1)).status());
        assertEquals(2, service.getAdminService().list().size());
        assertTrue(repository.getObject(EntityType.INFO, "' OR 1=1 --").isEmpty());
    }
}
