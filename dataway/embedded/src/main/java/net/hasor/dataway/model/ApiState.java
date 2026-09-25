/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Service view assembled from interface data and release snapshots; history is ordered oldest first. */
public class ApiState {
    private long             revision;
    private ApiDefinition    draft;
    private ApiRelease       published;
    private boolean          enabled;
    private List<ApiRelease> history = new ArrayList<>();

    public long getRevision() {
        return this.revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public ApiDefinition getDraft() {
        return this.draft;
    }

    public void setDraft(ApiDefinition draft) {
        this.draft = draft;
    }

    public ApiRelease getPublished() {
        return this.published;
    }

    public void setPublished(ApiRelease published) {
        this.published = published;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ApiRelease> getHistory() {
        return this.history;
    }

    public void setHistory(List<ApiRelease> history) {
        this.history = history;
    }

    public String getStatus() {
        if (this.published == null) {
            return "DRAFT";
        }

        if (!this.enabled) {
            return "DISABLED";
        }
        return Objects.equals(this.draft, this.published.getDefinition()) ? "PUBLISHED" : "MODIFIED";
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
        return this.revision == other.revision && this.enabled == other.enabled && Objects.equals(this.draft, other.draft) && Objects.equals(this.published, other.published) && Objects.equals(this.history, other.history);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.revision, this.draft, this.published, this.enabled, this.history);
    }

    @Override
    public String toString() {
        return "ApiState[revision=" + this.revision + ", draft=" + this.draft + ", published=" + this.published + ", enabled=" + this.enabled + ", history=" + this.history + "]";
    }
}
