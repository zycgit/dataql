/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Common storage contract for database, configuration-service and user implementations.
 * Reads follow the current transaction and must not expose other transactions' partial writes. Filters use exact, case-sensitive equality combined with AND.
 * Each returned record includes ID and REVISION. Providers must preserve unmodified fields.
 * INFO routes are unique by (METHOD, PATH); RELEASE permits historical copies of a route.
 */
public interface ApiDataAccessLayer {
    List<Map<FieldDef, String>> listObjects(EntityType entityType, Map<FieldDef, String> conditions);

    default Optional<Map<FieldDef, String>> getObject(EntityType entityType, String id) {
        return listObjects(entityType, Map.of(FieldDef.ID, id)).stream().findFirst();
    }

    /**
     * Entries are created through this access layer. Implementations validate them before writing.
     * Callers must not modify entries while write is running.
     * Apply all mutations atomically and in order, or apply none. When joining a host transaction,
     * final commit/rollback belongs to the host; failure must roll back or mark rollback-only.
     * CREATE starts at revision 1;
     * UPDATE compares version and increments it; DELETE compares before removing.
     * Conflicts throw DataConflictException. A process-local lock or sequential remote writes
     * do not satisfy this contract. Configuration stores must use an atomic CAS unit containing
     * all affected records (including the route index), or an equivalent transaction facility.
     */
    void write(List<DataMutation> mutations);

    default void createObject(EntityType entityType, String id, Map<FieldDef, String> fields) {
        write(List.of(create(entityType, OperationType.CREATE, id, 0, fields)));
    }

    default void updateObject(EntityType entityType, String id, long revision, Map<FieldDef, String> fields) {
        write(List.of(create(entityType, OperationType.UPDATE, id, revision, fields)));
    }

    default void deleteObject(EntityType entityType, String id, long revision) {
        write(List.of(create(entityType, OperationType.DELETE, id, revision, Map.of())));
    }

    /** Override to create a storage-specific subclass. All core writes use this factory. */
    default DataMutation create() {
        return new DataMutation();
    }

    /** Initialize an entry obtained from the overridable no-argument factory. */
    default DataMutation create(EntityType entityType, OperationType operationType, String id, long version, Map<FieldDef, String> fields) {
        DataMutation mutation = create();
        mutation.setEntityType(entityType);
        mutation.setOperationType(operationType);
        mutation.setId(id);
        mutation.setVersion(version);
        mutation.setFields(fields);
        mutation.validate();
        return mutation;
    }
}
