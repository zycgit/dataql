/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.config.ServiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest extends ServiceTestSupport {
    private AdminService service;

    @BeforeEach
    void createService() {
        BeanContainer beans = new BeanContainer();
        beans.setBean(ApiDataAccessLayer.class, this.access);
        this.service = new AdminServiceImpl(beans);
    }

    @Test
    void listOmitsDeletedApisAndScriptsWithoutChangingStorageRows() {
        Map<FieldDef, String> visible = this.info("visible", "0", 1);
        when(this.access.listObjects(EntityType.INFO, Map.of())).thenReturn(List.of(visible, this.info("deleted", "-1", 1)));
        List<ApiDefinition> result = this.service.list();
        assertEquals(1, result.size());
        assertEquals("visible", result.get(0).getId());
        assertNull(result.get(0).getScript());
        assertEquals("return 'value';", visible.get(SCRIPT));
    }

    @ParameterizedTest
    @CsvSource({ "0,false,false", "1,true,false", "2,true,true", "3,false,false" })
    void stateKeepsPublicationEnablementAndDraftChangesIndependent(String status, boolean enabled, boolean changed) {
        Map<FieldDef, String> info = this.info("api", status, 7);
        Map<FieldDef, String> release = this.release(info, "release", status.equals("3") ? "3" : "1", 10);
        if (changed) {
            info.put(SCRIPT, "return 'new';");
        }
        this.storeInfo(info);
        this.storeReleases("api", List.of(release));
        ApiState state = this.service.getApiById("api");
        assertEquals("api", state.getApiID());
        assertEquals(7, state.getRevision());
        assertTrue(state.isPublished());
        assertEquals(enabled, state.isEnabled());
        assertEquals(changed, state.isHasDraft());
        assertEquals(7, this.service.getVersionById("api"));
        assertEquals(info.get(SCRIPT), this.service.getDraftByApi("api").getScript());
    }

    @Test
    void unpublishedApiHasADraftAndNoRelease() {
        this.storeInfo(this.info("api", "0", 1));
        ApiState state = this.service.getApiById("api");
        assertFalse(state.isPublished());
        assertFalse(state.isEnabled());
        assertTrue(state.isHasDraft());
        assertNull(this.service.getReleaseByApi("api"));
        assertTrue(this.service.getHistoryByApi("api").isEmpty());
    }

    @Test
    void repeatedStateReadsCanBeDeduplicatedAndRetainTheirOwnRevision() {
        Map<FieldDef, String> info = this.info("api", "1", 7);
        this.storeInfo(info);
        this.storeReleases("api", List.of(this.release(info, "release", "1", 10)));

        ApiState first = this.service.getApiById("api");
        ApiState repeated = this.service.getApiById("api");
        assertNotSame(first, repeated);
        assertEquals(first, repeated);
        assertEquals(1, new HashSet<>(List.of(first, repeated)).size());

        info.put(REVISION, "8");
        info.put(SCRIPT, "return 'new draft';");
        ApiState changed = this.service.getApiById("api");
        assertNotEquals(first, changed);
        assertEquals(7, first.getRevision());
        assertFalse(first.isHasDraft());
        assertEquals(8, changed.getRevision());
        assertTrue(changed.isHasDraft());
    }

    @ParameterizedTest
    @ValueSource(strings = { "script", "schema", "sample", "options", "description" })
    void editingADraftSnapshotDoesNotMutateOtherReadsOrStorage(String field) {
        Map<FieldDef, String> info = this.info("api", "0", 1);
        this.storeInfo(info);
        ApiDefinition original = this.service.getDraftByApi("api");
        ApiDefinition editable = this.service.getDraftByApi("api");
        assertNotSame(original, editable);
        assertEquals(original, editable);
        assertEquals(1, new HashSet<>(List.of(original, editable)).size());

        switch (field) {
            case "script" -> editable.setScript("return 'edited';");
            case "schema" -> editable.setSchema("{\"type\":\"object\"}");
            case "sample" -> editable.setSample("{\"requestBody\":{\"id\":1}}");
            case "options" -> editable.setOptions("{\"resultStructure\":false}");
            case "description" -> editable.setDescription("Edited description");
            default -> throw new AssertionError("Unexpected field: " + field);
        }

        assertNotEquals(original, editable);
        assertEquals(original, this.service.getDraftByApi("api"));
        verify(this.access, never()).write(anyList());
    }

    @Test
    void historyAndReleaseReadsReturnEqualButIndependentSnapshots() {
        Map<FieldDef, String> info = this.info("api", "1", 1);
        Map<FieldDef, String> release = this.release(info, "release", "1", 10);
        this.storeInfo(info);
        this.storeReleases("api", List.of(release));
        doReturn(Optional.of(release)).when(this.access).getObject(EntityType.RELEASE, "release");

        ApiRelease published = this.service.getReleaseByApi("api");
        ApiRelease historical = this.service.getHistoryById("release");
        assertNotSame(published, historical);
        assertNotSame(published.getDefinition(), historical.getDefinition());
        assertEquals(published, historical);
        assertEquals(1, new HashSet<>(List.of(published, historical)).size());

        historical.getDefinition().setScript("return 'local edit';");
        assertNotEquals(published, historical);
        assertEquals("return 'value';", published.getDefinition().getScript());
        assertEquals(published, this.service.getReleaseById("release"));
        verify(this.access, never()).write(anyList());
    }

    @Test
    void stateRetriesAReadThatCrossedAnUpdate() {
        Map<FieldDef, String> before = this.info("api", "0", 1);
        Map<FieldDef, String> after = this.info("api", "0", 2);
        doReturn(Optional.of(before), Optional.of(after), Optional.of(after)).when(this.access).getObject(EntityType.INFO, "api");
        assertEquals(2, this.service.getApiById("api").getRevision());
        verify(this.access, times(4)).getObject(EntityType.INFO, "api");
    }

    @Test
    void continuouslyChangingRevisionsFailAfterThreeAttempts() {
        Map<FieldDef, String> first = this.info("api", "0", 1);
        Map<FieldDef, String> second = this.info("api", "0", 2);
        doReturn(Optional.of(first), Optional.of(second), Optional.of(first), Optional.of(second), Optional.of(first), Optional.of(second)).when(this.access).getObject(EntityType.INFO, "api");
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.getApiById("api")).status());
        verify(this.access, times(6)).getObject(EntityType.INFO, "api");
    }

    @Test
    void deletedOrMissingApisAndReleasesAreNotReadable() {
        this.storeInfo(this.info("deleted", "-1", 2));
        assertEquals(404, assertThrows(DatawayException.class, () -> this.service.getDraftByApi("deleted")).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> this.service.getVersionById("missing")).status());
        assertEquals(404, assertThrows(DatawayException.class, () -> this.service.getHistoryById("missing")).status());
        Map<FieldDef, String> deleted = this.release(this.info("api", "1", 1), "deleted-release", "-1", 1);
        doReturn(Optional.of(deleted)).when(this.access).getObject(EntityType.RELEASE, "deleted-release");
        assertEquals(404, assertThrows(DatawayException.class, () -> this.service.getHistoryById("deleted-release")).status());
    }

    @Test
    void historyIsOrderedByTimeThenIdAndAnActiveReleaseWinsOverANewerDisabledRelease() {
        Map<FieldDef, String> info = this.info("api", "1", 3);
        this.storeInfo(info);
        Map<FieldDef, String> a = this.release(info, "a", "1", 10);
        Map<FieldDef, String> b = this.release(info, "b", "3", 10);
        Map<FieldDef, String> c = this.release(info, "c", "3", 20);
        this.storeReleases("api", List.of(c, b, this.release(info, "deleted", "-1", 5), a));
        doReturn(Optional.of(b)).when(this.access).getObject(EntityType.RELEASE, "b");

        List<ApiRelease> history = this.service.getHistoryByApi("api");
        assertEquals(List.of("a", "b", "c"), history.stream().map(ApiRelease::getId).toList());
        assertEquals(List.of(1L, 2L, 3L), history.stream().map(ApiRelease::getNumber).toList());
        assertEquals("api", history.get(0).getDefinition().getId());
        assertEquals(10, history.get(0).getPublishedAt().toEpochMilli());
        assertEquals("a", this.service.getReleaseByApi("api").getId());
        assertEquals(2, this.service.getReleaseById("b").getNumber());
        a.put(STATUS, "3");
        assertEquals("c", this.service.getReleaseByApi("api").getId());
    }

    @Test
    void historyDisappearingBetweenIndependentReadsIsNotReturned() {
        Map<FieldDef, String> info = this.info("api", "1", 1);
        this.storeInfo(info);
        doReturn(Optional.of(this.release(info, "missing", "1", 1))).when(this.access).getObject(EntityType.RELEASE, "missing");
        assertEquals(404, assertThrows(DatawayException.class, () -> this.service.getHistoryById("missing")).status());
    }

    @Test
    void newSqlDraftStoresOnlyTheOriginalScriptAndInitialMetadata() {
        ApiDefinition definition = this.definition("api", "select :id");
        definition.setType(ApiScriptType.SQL);
        ApiState saved = this.service.save(definition, 0);
        assertEquals(1, saved.getRevision());
        assertFalse(saved.isPublished());
        assertTrue(saved.isHasDraft());
        DataMutation change = this.changes().get(0);
        assertEquals(OperationType.CREATE, change.getOperationType());
        assertEquals(0, change.getVersion());
        assertEquals("select :id", change.getFields().get(SCRIPT));
        assertEquals("SQL", change.getFields().get(TYPE));
        assertEquals("0", change.getFields().get(STATUS));
        assertEquals("{}", change.getFields().get(SCHEMA));
        assertEquals(change.getFields().get(CREATE_TIME), change.getFields().get(GMT_TIME));
    }

    private List<DataMutation> changes() {
        ArgumentCaptor<List<DataMutation>> captured = ArgumentCaptor.captor();
        verify(this.access).write(captured.capture());
        return captured.getValue();
    }

    @Test
    void draftUpdatesRetainUnspecifiedDocumentsAndDoNotAlterThePublication() {
        Map<FieldDef, String> info = this.info("api", "1", 4);
        info.put(SCHEMA, "{\"type\":\"object\"}");
        info.put(SAMPLE, "{\"requestBody\":{\"id\":1}}");
        info.put(OPTION, "{\"resultStructure\":false}");
        this.storeInfo(info);
        Map<FieldDef, String> release = this.release(info, "old", "1", 1);
        this.storeReleases("api", List.of(release));
        ApiDefinition incoming = this.definition("api", "return 'changed';");
        ApiState saved = this.service.save(incoming, 4);
        assertTrue(saved.isEnabled());
        assertTrue(saved.isHasDraft());
        List<DataMutation> changes = this.changes();
        assertEquals(1, changes.size());
        assertEquals("2", changes.get(0).getFields().get(STATUS));
        assertEquals(info.get(SCHEMA), changes.get(0).getFields().get(SCHEMA));
        assertEquals(info.get(SAMPLE), changes.get(0).getFields().get(SAMPLE));
        assertEquals(info.get(OPTION), changes.get(0).getFields().get(OPTION));
        assertFalse(changes.get(0).getFields().containsKey(CREATE_TIME));
        assertEquals("return 'value';", release.get(SCRIPT));
        assertNull(incoming.getSchema());
    }

    @Test
    void unchangedDraftRemainsPublishedAndExplicitDocumentsReplacePreviousValues() {
        Map<FieldDef, String> info = this.info("api", "1", 2);
        this.storeInfo(info);
        this.storeReleases("api", List.of(this.release(info, "old", "1", 1)));
        ApiDefinition definition = this.service.getDraftByApi("api");
        ApiState state = this.service.save(definition, 2);
        assertFalse(state.isHasDraft());
        assertEquals("1", this.changes().get(0).getFields().get(STATUS));
    }

    @Test
    void savingADisabledApiDisablesAnyInconsistentActiveReleasesInTheSameBatch() {
        Map<FieldDef, String> info = this.info("api", "3", 2);
        this.storeInfo(info);
        this.storeReleases("api", List.of(this.release(info, "active", "1", 1), this.release(info, "inactive", "3", 2)));
        this.service.save(this.definition("api", "return 'changed';"), 2);
        List<DataMutation> changes = this.changes();
        assertEquals(2, changes.size());
        assertEquals("3", changes.get(0).getFields().get(STATUS));
        assertEquals("active", changes.get(1).getId());
        assertEquals(Map.of(STATUS, "3"), changes.get(1).getFields());
    }

    @ParameterizedTest
    @ValueSource(strings = { "method", "path" })
    void routeChangesAreRejectedBeforeAnyWrite(String change) {
        this.storeInfo(this.info("api", "0", 1));
        ApiDefinition incoming = this.definition("api", "return 1;");
        if (change.equals("method")) {
            incoming.setMethod("POST");
        } else {
            incoming.setPath("/other");
        }
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.save(incoming, 1)).status());
        verify(this.access, never()).write(anyList());
    }

    @Test
    void staleVersionsNeverWriteAndProviderConflictsRetainTheirCause() {
        this.storeInfo(this.info("api", "0", 4));
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.save(this.definition("api", "return 1;"), 3)).status());
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.publish("api", 3)).status());
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.disableApi("api", 3)).status());
        assertEquals(409, assertThrows(DatawayException.class, () -> this.service.deleteApi("api", 3)).status());
        verify(this.access, never()).write(anyList());

        DataConflictException conflict = new DataConflictException("CAS lost");
        doThrow(conflict).when(this.access).write(anyList());
        DatawayException mapped = assertThrows(DatawayException.class, () -> this.service.publish("api", 4));
        assertEquals(409, mapped.status());
        assertSame(conflict, mapped.getCause());
    }

    @Test
    void publicationUpdatesTheDraftAndReleasesAtomicallyWithIncreasingTime() {
        Map<FieldDef, String> info = this.info("api", "2", 8);
        this.storeInfo(info);
        long future = System.currentTimeMillis() + 60_000;
        Map<FieldDef, String> old = this.release(info, "old", "1", future);
        this.storeReleases("api", List.of(old));
        ApiState state = this.service.publish("api", 8);
        assertEquals(9, state.getRevision());
        assertTrue(state.isEnabled());
        assertFalse(state.isHasDraft());

        List<DataMutation> changes = this.changes();
        assertEquals(3, changes.size());
        assertEquals(EntityType.INFO, changes.get(0).getEntityType());
        assertEquals(8, changes.get(0).getVersion());
        assertEquals("old", changes.get(1).getId());
        DataMutation release = changes.get(2);
        assertEquals(OperationType.CREATE, release.getOperationType());
        assertEquals(EntityType.RELEASE, release.getEntityType());
        assertEquals("api", release.getFields().get(API_ID));
        assertEquals(info.get(SCRIPT), release.getFields().get(SCRIPT));
        assertEquals(Long.toString(future + 1), release.getFields().get(RELEASE_TIME));
        assertEquals("1", old.get(STATUS));
    }

    @Test
    void initialPublicationNeedsNoExistingReleaseAndStorageFailuresAreNotHidden() {
        this.storeInfo(this.info("api", "0", 1));
        DataAccessException failure = new DataAccessException("offline", null);
        doThrow(failure).when(this.access).write(anyList());
        assertSame(failure, assertThrows(DataAccessException.class, () -> this.service.publish("api", 1)));
        assertEquals(2, this.changes().size());
    }

    @Test
    void disableKeepsHistoryWhileDeleteRemovesAllHistoryIncludingTombstones() {
        Map<FieldDef, String> info = this.info("api", "1", 1);
        this.storeInfo(info);
        this.storeReleases("api", List.of(this.release(info, "active", "1", 1)));
        ApiState disabled = this.service.disableApi("api", 1);
        assertTrue(disabled.isPublished());
        assertFalse(disabled.isEnabled());
        assertEquals(2, this.changes().size());

        clearInvocations(this.access);
        this.storeReleases("api", List.of(this.release(info, "active", "1", 1), this.release(info, "removed", "-1", 2)));
        this.service.deleteApi("api", 1);
        List<DataMutation> changes = this.changes();
        assertEquals(List.of("api", "active", "removed"), changes.stream().map(DataMutation::getId).toList());
        assertTrue(changes.stream().allMatch(change -> change.getOperationType() == OperationType.DELETE));
    }

    @Test
    void disablingAnUnpublishedApiKeepsItUnpublished() {
        this.storeInfo(this.info("api", "0", 1));
        assertFalse(this.service.disableApi("api", 1).isPublished());
        assertEquals("0", this.changes().get(0).getFields().get(STATUS));
    }
}
