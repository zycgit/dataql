/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.Objects;

/** Paths are exact, case-sensitive and relative to the adapter's API prefix. */
public class ApiDefinition {
    private String        id;
    private String        method;
    private String        path;
    private ApiScriptType type;
    private String        script;
    private String        description;
    private String        schema;
    private String        sample;
    private String        options;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ApiScriptType getType() {
        return this.type;
    }

    public void setType(ApiScriptType type) {
        this.type = type;
    }

    public String getScript() {
        return this.script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchema() {
        return this.schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getSample() {
        return this.sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getOptions() {
        return this.options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        ApiDefinition other = (ApiDefinition) object;
        return Objects.equals(this.id, other.id) && Objects.equals(this.method, other.method) && Objects.equals(this.path, other.path) && this.type == other.type && Objects.equals(this.script, other.script) && Objects.equals(this.description, other.description) && Objects.equals(this.schema, other.schema) && Objects.equals(this.sample, other.sample) && Objects.equals(this.options, other.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.method, this.path, this.type, this.script, this.description, this.schema, this.sample, this.options);
    }

    @Override
    public String toString() {
        return "ApiDefinition[id=" + this.id + ", method=" + this.method + ", path=" + this.path + ", type=" + this.type + ", script=" + this.script + ", description=" + this.description + ", schema=" + this.schema + ", sample=" + this.sample + ", options=" + this.options + "]";
    }
}
