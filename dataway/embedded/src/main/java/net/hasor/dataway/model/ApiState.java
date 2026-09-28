/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.Objects;

/** API identity, revision and publication flags, independent of drafts and release contents. */
public class ApiState {
    private String  apiID;
    private long    revision;
    private boolean published;
    private boolean enabled;
    private boolean hasDraft;

    public String getApiID() {
        return this.apiID;
    }

    public void setApiID(String apiID) {
        this.apiID = apiID;
    }

    public long getRevision() {
        return this.revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public boolean isPublished() {
        return this.published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Whether the editable definition has changes that have not been published. */
    public boolean isHasDraft() {
        return this.hasDraft;
    }

    public void setHasDraft(boolean hasDraft) {
        this.hasDraft = hasDraft;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }
        ApiState other = (ApiState) object;
        return Objects.equals(this.apiID, other.apiID) && this.revision == other.revision && this.published == other.published && this.enabled == other.enabled && this.hasDraft == other.hasDraft;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.apiID, this.revision, this.published, this.enabled, this.hasDraft);
    }

    @Override
    public String toString() {
        return "ApiState[apiID=" + this.apiID + ", revision=" + this.revision + ", published=" + this.published + ", enabled=" + this.enabled + ", hasDraft=" + this.hasDraft + "]";
    }
}
