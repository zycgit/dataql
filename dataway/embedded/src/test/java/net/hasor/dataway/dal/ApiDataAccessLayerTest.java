/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ApiDataAccessLayerTest {
    private ApiDataAccessLayer access;

    @BeforeEach
    void createAccessLayer() {
        this.access = mock(ApiDataAccessLayer.class, CALLS_REAL_METHODS);
    }

    @Test
    void emptyMappingsDoNotRequireProviderSupport() {
        assertDoesNotThrow(() -> this.access.configureMapping(Map.of(), Map.of()));
    }

    @Test
    void unsupportedTableAndFieldMappingsFailExplicitly() {
        Map<EntityType, String> tables = Map.of(EntityType.INFO, "custom_info");
        Map<EntityType, Map<FieldDef, String>> fields = Map.of(EntityType.RELEASE, Map.of(FieldDef.SCRIPT, "SCRIPT_ORI"));

        assertThrows(UnsupportedOperationException.class, () -> this.access.configureMapping(tables, Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> this.access.configureMapping(Map.of(), fields));
        assertThrows(UnsupportedOperationException.class, () -> this.access.configureMapping(tables, fields));
        verify(this.access, never()).write(anyList());
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void lookupUsesOnlyTheExactIdAndPreservesProviderFields(EntityType entityType) {
        String id = "Api/A?x=1'";
        Map<FieldDef, String> row = Map.of(FieldDef.ID, id, FieldDef.REVISION, "9", FieldDef.SCRIPT, "return 1;");
        when(this.access.listObjects(entityType, Map.of(FieldDef.ID, id))).thenReturn(List.of(row));

        assertSame(row, this.access.getObject(entityType, id).orElseThrow());
        verify(this.access).listObjects(entityType, Map.of(FieldDef.ID, id));
    }

    @Test
    void missingRecordReturnsAnEmptyOptional() {
        when(this.access.listObjects(EntityType.INFO, Map.of(FieldDef.ID, "missing"))).thenReturn(List.of());

        assertEquals(Optional.empty(), this.access.getObject(EntityType.INFO, "missing"));
    }

    @Test
    void lookupDoesNotSwallowStorageFailures() {
        DataAccessException failure = new DataAccessException("Storage unavailable", new IllegalStateException("offline"));
        when(this.access.listObjects(EntityType.RELEASE, Map.of(FieldDef.ID, "api"))).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessException.class, () -> this.access.getObject(EntityType.RELEASE, "api")));
    }

    @Test
    void defaultFactoryCreatesIndependentMutableEntries() {
        DataMutation first = this.access.create();
        DataMutation second = this.access.create();
        first.getFields().put(FieldDef.COMMENT, "draft");

        assertNotSame(first, second);
        assertTrue(second.getFields().isEmpty());
    }

    @Test
    void initializedFactoryUsesAndValidatesTheProviderEntry() {
        DataMutation supplied = spy(this.access.create());
        doReturn(supplied).when(this.access).create();
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(FieldDef.SCRIPT, "select :id");

        DataMutation actual = this.access.create(EntityType.RELEASE, OperationType.UPDATE, "release", 7, fields);

        assertSame(supplied, actual);
        assertEquals(EntityType.RELEASE, actual.getEntityType());
        assertEquals(OperationType.UPDATE, actual.getOperationType());
        assertEquals("release", actual.getId());
        assertEquals(7, actual.getVersion());
        assertSame(fields, actual.getFields());
        verify(supplied).validate();
    }

    @Test
    void createWritesOneEntryWithInitialVersionZero() {
        Map<FieldDef, String> fields = Map.of(FieldDef.METHOD, "POST", FieldDef.PATH, "/orders");

        this.access.createObject(EntityType.INFO, "api", fields);

        DataMutation mutation = this.writtenMutation();
        assertEquals(EntityType.INFO, mutation.getEntityType());
        assertEquals(OperationType.CREATE, mutation.getOperationType());
        assertEquals("api", mutation.getId());
        assertEquals(0, mutation.getVersion());
        assertSame(fields, mutation.getFields());
    }

    private DataMutation writtenMutation() {
        ArgumentCaptor<List<DataMutation>> entries = ArgumentCaptor.captor();
        verify(this.access).write(entries.capture());
        assertEquals(1, entries.getValue().size());
        return entries.getValue().get(0);
    }

    @Test
    void updatePreservesTheExpectedVersionAndExplicitNulls() {
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(FieldDef.COMMENT, null);

        this.access.updateObject(EntityType.INFO, "api", 19, fields);

        DataMutation mutation = this.writtenMutation();
        assertEquals(EntityType.INFO, mutation.getEntityType());
        assertEquals(OperationType.UPDATE, mutation.getOperationType());
        assertEquals("api", mutation.getId());
        assertEquals(19, mutation.getVersion());
        assertSame(fields, mutation.getFields());
        assertTrue(mutation.getFields().containsKey(FieldDef.COMMENT));
        assertNull(mutation.getFields().get(FieldDef.COMMENT));
        assertFalse(mutation.getFields().containsKey(FieldDef.SCRIPT));
    }

    @Test
    void deleteWritesOnlyTheIdentityAndExpectedVersion() {
        this.access.deleteObject(EntityType.RELEASE, "release", 3);

        DataMutation mutation = this.writtenMutation();
        assertEquals(EntityType.RELEASE, mutation.getEntityType());
        assertEquals(OperationType.DELETE, mutation.getOperationType());
        assertEquals("release", mutation.getId());
        assertEquals(3, mutation.getVersion());
        assertTrue(mutation.getFields().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(OperationType.class)
    void allWriteConveniencesRespectTheOverridableFactory(OperationType operation) {
        DataMutation supplied = spy(this.access.create());
        doReturn(supplied).when(this.access).create();
        switch (operation) {
            case CREATE -> {
                this.access.createObject(EntityType.INFO, "api", Map.of());
            }
            case UPDATE -> {
                this.access.updateObject(EntityType.INFO, "api", 2, Map.of());
            }
            case DELETE -> {
                this.access.deleteObject(EntityType.INFO, "api", 2);
            }
        }

        verify(this.access).write(List.of(supplied));
        verify(supplied).validate();
        assertEquals(operation, supplied.getOperationType());
    }

    @Test
    void invalidEntriesNeverReachTheProviderWrite() {
        assertThrows(IllegalArgumentException.class, () -> this.access.createObject(EntityType.INFO, " ", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> this.access.updateObject(EntityType.INFO, "api", 0, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> this.access.deleteObject(EntityType.INFO, "api", 0));
        verify(this.access, never()).write(anyList());
    }

    @Test
    void providerValidationCanRejectAnEntryBeforeWrite() {
        DataMutation supplied = spy(this.access.create());
        doReturn(supplied).when(this.access).create();
        IllegalArgumentException failure = new IllegalArgumentException("Provider rejected the field");
        doThrow(failure).when(supplied).validate();

        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> this.access.createObject(EntityType.INFO, "api", Map.of(FieldDef.SCRIPT, "return 1;"))));
        verify(this.access, never()).write(anyList());
    }

    @Test
    void conflictsReachTheCallerWithoutRetryingOrChangingVersion() {
        DataConflictException failure = new DataConflictException("Stale revision");
        doThrow(failure).when(this.access).write(anyList());

        assertSame(failure, assertThrows(DataConflictException.class, () -> this.access.updateObject(EntityType.INFO, "api", 5, Map.of(FieldDef.COMMENT, "update"))));
        assertEquals(5, this.writtenMutation().getVersion());
    }
}
