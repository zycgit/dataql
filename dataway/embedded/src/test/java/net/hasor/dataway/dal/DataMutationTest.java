/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class DataMutationTest {
    private final ApiDataAccessLayer access = mock(ApiDataAccessLayer.class, CALLS_REAL_METHODS);

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void acceptsBothEntityTypesAndKeepsOriginalScript(EntityType entityType) {
        String script = "select * from orders where id = :id";
        DataMutation mutation = this.access.create(entityType, OperationType.CREATE, "api", 0, Map.of(FieldDef.SCRIPT, script));

        assertEquals(entityType, mutation.getEntityType());
        assertEquals(script, mutation.getFields().get(FieldDef.SCRIPT));
        assertDoesNotThrow(mutation::validate);
    }

    @Test
    void entityAndOperationAreRequired() {
        assertThrows(NullPointerException.class, () -> this.access.create(null, OperationType.CREATE, "api", 0, Map.of()));
        assertThrows(NullPointerException.class, () -> this.access.create(EntityType.INFO, null, "api", 0, Map.of()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t\r\n", "\u2003" })
    void rejectsMissingOrBlankIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.INFO, OperationType.CREATE, id, 0, Map.of()));
    }

    @Test
    void acceptsIdsUpTo64CharactersWithoutChangingTheirContents() {
        String id = "a".repeat(64);
        assertEquals(id, this.access.create(EntityType.INFO, OperationType.CREATE, id, 0, Map.of()).getId());
        assertEquals(" api ", this.access.create(EntityType.INFO, OperationType.CREATE, " api ", 0, Map.of()).getId());
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.INFO, OperationType.CREATE, id + "a", 0, Map.of()));
    }

    @ParameterizedTest
    @CsvSource({ "CREATE,0", "UPDATE,1", "DELETE,1", "UPDATE,9223372036854775807", "DELETE,9223372036854775807" })
    void acceptsTheVersionRequiredByEachOperation(OperationType operation, long version) {
        DataMutation mutation = this.access.create(EntityType.INFO, operation, "api", version, Map.of());
        assertEquals(version, mutation.getVersion());
        assertDoesNotThrow(mutation::validate);
    }

    @ParameterizedTest
    @CsvSource({ "CREATE,-1", "CREATE,1", "CREATE,9223372036854775807", "UPDATE,0", "UPDATE,-1", "DELETE,0", "DELETE,-1" })
    void rejectsVersionsThatCannotRepresentTheOperation(OperationType operation, long version) {
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.INFO, operation, "api", version, Map.of()));
    }

    @ParameterizedTest
    @EnumSource(value = FieldDef.class, names = { "ID", "REVISION" })
    void controlledFieldsCannotBeWrittenEvenWithNullValues(FieldDef field) {
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(field, null);
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.INFO, OperationType.CREATE, "api", 0, fields));
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.INFO, OperationType.UPDATE, "api", 1, fields));
    }

    @Test
    void deleteRejectsAnyFieldPayload() {
        assertThrows(IllegalArgumentException.class, () -> this.access.create(EntityType.RELEASE, OperationType.DELETE, "release", 1, Map.of(FieldDef.STATUS, "0")));
    }

    @Test
    void fieldMapRemainsExtensibleAndDistinguishesClearedFromUnchanged() {
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(FieldDef.COMMENT, null);
        DataMutation mutation = this.access.create(EntityType.INFO, OperationType.UPDATE, "api", 1, fields);
        fields.put(FieldDef.SCRIPT, "return null;");

        assertSame(fields, mutation.getFields());
        assertTrue(mutation.getFields().containsKey(FieldDef.COMMENT));
        assertNull(mutation.getFields().get(FieldDef.COMMENT));
        assertEquals("return null;", mutation.getFields().get(FieldDef.SCRIPT));
        assertFalse(mutation.getFields().containsKey(FieldDef.PATH));
        assertDoesNotThrow(mutation::validate);
    }

    @Test
    void validationDetectsInvalidChangesAfterCreation() {
        DataMutation mutation = this.access.create(EntityType.INFO, OperationType.UPDATE, "api", 3, new EnumMap<>(FieldDef.class));
        mutation.setVersion(0);
        assertThrows(IllegalArgumentException.class, mutation::validate);
        mutation.setVersion(3);
        mutation.getFields().put(FieldDef.REVISION, "4");
        assertThrows(IllegalArgumentException.class, mutation::validate);
    }
}
