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
import java.util.Objects;

/** A batch write entry, created through ApiDataAccessLayer and extensible by storage providers. */
public class DataMutation {
    private String                id;
    private EntityType            entityType;
    private OperationType         operationType;
    private Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
    private long                  version;

    protected DataMutation() {
    }

    /** Validate before writing; omitted fields are unchanged, present null values explicitly clear fields. */
    public void validate() {
        Objects.requireNonNull(this.getEntityType(), "entityType");
        Objects.requireNonNull(this.getOperationType(), "operationType");
        String id = this.getId();
        if (id == null || id.isBlank() || id.length() > 64) {
            throw new IllegalArgumentException("Invalid record id");
        }

        long revision = this.getVersion();
        if (this.getOperationType() == OperationType.CREATE ? revision != 0 : revision < 1) {
            throw new IllegalArgumentException("Invalid expected revision");
        }

        Map<FieldDef, String> fields = this.getFields();
        if (fields.containsKey(FieldDef.ID) || fields.containsKey(FieldDef.REVISION)) {
            throw new IllegalArgumentException("ID and revision are controlled by the mutation");
        }
        if (this.getOperationType() == OperationType.DELETE && !fields.isEmpty()) {
            throw new IllegalArgumentException("Delete cannot contain fields");
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Map<FieldDef, String> getFields() {
        return fields;
    }

    public void setFields(Map<FieldDef, String> fields) {
        this.fields = fields;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
