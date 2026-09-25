/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.time.Instant;
import java.util.Objects;

public class ApiRelease {
    private String        id;
    private long          number;
    private Instant       publishedAt;
    private ApiDefinition definition;
    private String        executionScript;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getNumber() {
        return this.number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public ApiDefinition getDefinition() {
        return this.definition;
    }

    public void setDefinition(ApiDefinition definition) {
        this.definition = definition;
    }

    public String getExecutionScript() {
        return this.executionScript;
    }

    public void setExecutionScript(String executionScript) {
        this.executionScript = executionScript;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        ApiRelease other = (ApiRelease) object;
        return this.number == other.number && Objects.equals(this.id, other.id) && Objects.equals(this.publishedAt, other.publishedAt) && Objects.equals(this.definition, other.definition) && Objects.equals(this.executionScript, other.executionScript);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.number, this.publishedAt, this.definition, this.executionScript);
    }

    @Override
    public String toString() {
        return "ApiRelease[id=" + this.id + ", number=" + this.number + ", publishedAt=" + this.publishedAt + ", definition=" + this.definition + ", executionScript=" + this.executionScript + "]";
    }
}
