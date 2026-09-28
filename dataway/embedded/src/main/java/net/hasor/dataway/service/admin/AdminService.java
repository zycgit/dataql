/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.util.List;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.model.ApiState;

/** Public API for managing API drafts, releases and history. */
public interface AdminService {
    /** Lists editable API definitions without script contents. */
    List<ApiDefinition> list();

    /** Returns only the revision and publication flags of the specified API. */
    ApiState getApiById(String apiID);

    /** Returns the current API version used for optimistic concurrency control. */
    long getVersionById(String apiID);

    /** Returns the editable definition, including its original script. */
    ApiDefinition getDraftByApi(String apiID);

    /** Returns one publication snapshot by its history ID. */
    ApiRelease getHistoryById(String historyID);

    /** Returns all publication snapshots, ordered oldest first. */
    List<ApiRelease> getHistoryByApi(String apiID);

    /** Returns a publication snapshot; release and history IDs identify the same stored record. */
    ApiRelease getReleaseById(String releaseID);

    /** Returns the current or last publication, even when disabled; null if never published. */
    ApiRelease getReleaseByApi(String apiID);

    /** Saves a draft using the expected version, or zero for a new API. */
    ApiState save(ApiDefinition definition, long version);

    /** Publishes the saved draft using the expected version. */
    ApiState publish(String apiID, long version);

    /** Disables the API using the expected version. */
    ApiState disableApi(String apiID, long version);

    /** Deletes the API and its release history using the expected version. */
    void deleteApi(String apiID, long version);
}
