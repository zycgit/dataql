/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.time.Instant;
import java.util.*;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiRelease;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import static net.hasor.dataway.dal.FieldDef.*;

/** Manages API drafts, releases and history through the DAL. */
public class AdminServiceImpl implements AdminService {
    private final ApiDataAccessLayer access;

    public AdminServiceImpl(BeanContainer beans) {
        this.access = beans.getBean(ApiDataAccessLayer.class);
    }

    @Override
    public List<ApiDefinition> list() {
        List<ApiDefinition> definitions = new ArrayList<>();
        for (var row : this.access.listObjects(EntityType.INFO, Map.of())) {
            if (!"-1".equals(row.get(STATUS))) {
                ApiDefinition definition = this.definition(row, false);
                definition.setScript(null);
                definitions.add(definition);
            }
        }
        return definitions;
    }

    private ApiDefinition definition(Map<FieldDef, String> row, boolean release) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(row.get(release ? API_ID : ID));
        definition.setMethod(row.get(METHOD));
        definition.setPath(row.get(PATH));
        definition.setType(ApiScriptType.fromName(row.get(TYPE)));
        definition.setScript(row.get(SCRIPT));
        definition.setDescription(row.get(COMMENT));
        definition.setSchema(row.get(SCHEMA));
        definition.setSample(row.get(SAMPLE));
        definition.setOptions(row.get(OPTION));
        return definition;
    }

    @Override
    public ApiState getApiById(String apiID) {
        // A publication updates INFO and RELEASE together; retry if separate reads cross that update.
        for (int attempt = 0; attempt < 3; attempt++) {
            var info = this.info(apiID);
            var published = this.latest(this.releases(apiID));
            var after = this.findInfo(apiID);
            if (after != null && Objects.equals(info.get(REVISION), after.get(REVISION))) {
                boolean enabled = published != null && "1".equals(published.get(STATUS)) && Set.of("1", "2").contains(info.get(STATUS));
                return this.state(this.revision(info), this.definition(info, false), published, enabled);
            }
        }
        throw new DatawayException(409, "API changed while reading; reload and retry");
    }

    private Map<FieldDef, String> info(String apiID) {
        var info = this.findInfo(apiID);
        if (info == null) {
            throw new DatawayException(404, "API not found");
        }
        return info;
    }

    private Map<FieldDef, String> findInfo(String apiID) {
        var info = this.access.getObject(EntityType.INFO, apiID).orElse(null);
        if (info == null || "-1".equals(info.get(STATUS))) {
            return null;
        }
        return info;
    }

    private Map<FieldDef, String> latest(List<Map<FieldDef, String>> releases) {
        Map<FieldDef, String> active = null;
        for (var row : releases) {
            if ("1".equals(row.get(STATUS))) {
                active = row;
            }
        }
        if (active != null) {
            return active;
        }
        return releases.isEmpty() ? null : releases.get(releases.size() - 1);
    }

    private List<Map<FieldDef, String>> releases(String apiID) {
        List<Map<FieldDef, String>> rows = new ArrayList<>(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, apiID)));
        rows.removeIf(row -> "-1".equals(row.get(STATUS)));
        rows.sort(Comparator.<Map<FieldDef, String>>comparingLong(row -> {
            return Long.parseLong(row.get(RELEASE_TIME));
        }).thenComparing(row -> {
            return row.get(ID);
        }));
        return rows;
    }

    private long revision(Map<FieldDef, String> row) {
        return Long.parseLong(row.get(REVISION));
    }

    private ApiState state(long revision, ApiDefinition draft, Map<FieldDef, String> published, boolean enabled) {
        ApiState state = new ApiState();
        state.setApiID(draft.getId());
        state.setRevision(revision);
        state.setPublished(published != null);
        state.setEnabled(enabled);
        state.setHasDraft(published == null || !Objects.equals(draft, this.definition(published, true)));
        return state;
    }

    @Override
    public long getVersionById(String apiID) {
        return this.revision(this.info(apiID));
    }

    @Override
    public ApiDefinition getDraftByApi(String apiID) {
        return this.definition(this.info(apiID), false);
    }

    @Override
    public ApiRelease getHistoryById(String historyID) {
        var row = this.access.getObject(EntityType.RELEASE, historyID).orElse(null);
        if (row == null || "-1".equals(row.get(STATUS))) {
            throw new DatawayException(404, "Release not found");
        }
        String apiID = row.get(API_ID);
        this.info(apiID);
        var rows = this.releases(apiID);
        for (int i = 0; i < rows.size(); i++) {
            if (historyID.equals(rows.get(i).get(ID))) {
                return this.release(rows.get(i), i + 1L);
            }
        }
        throw new DatawayException(404, "Release not found");
    }

    private ApiRelease release(Map<FieldDef, String> row, long number) {
        ApiRelease release = new ApiRelease();
        release.setId(row.get(ID));
        release.setNumber(number);
        release.setPublishedAt(Instant.ofEpochMilli(Long.parseLong(row.get(RELEASE_TIME))));
        release.setDefinition(this.definition(row, true));
        return release;
    }

    @Override
    public List<ApiRelease> getHistoryByApi(String apiID) {
        this.info(apiID);
        List<ApiRelease> history = new ArrayList<>();
        for (var row : this.releases(apiID)) {
            history.add(this.release(row, history.size() + 1L));
        }
        return history;
    }

    @Override
    public ApiRelease getReleaseById(String releaseID) {
        return this.getHistoryById(releaseID);
    }

    @Override
    public ApiRelease getReleaseByApi(String apiID) {
        this.info(apiID);
        var rows = this.releases(apiID);
        var published = this.latest(rows);
        if (published == null) {
            return null;
        }
        return this.release(published, rows.indexOf(published) + 1L);
    }

    @Override
    public ApiState save(ApiDefinition definition, long version) {
        String apiID = definition.getId();
        var previous = this.findInfo(apiID);
        this.checkVersion(previous, version);
        if (previous != null && (!StringUtils.equalsIgnoreCase(previous.get(METHOD), definition.getMethod()) || !previous.get(PATH).equals(definition.getPath()))) {
            throw new DatawayException(409, "API route is immutable; create a new API for a different route");
        }

        ApiDefinition draft = this.complete(definition, previous);
        var releases = this.releases(apiID);
        var published = this.latest(releases);
        boolean enabled = previous != null && published != null && "1".equals(published.get(STATUS)) && Set.of("1", "2").contains(previous.get(STATUS));
        ApiState next = this.state(version + 1, draft, published, enabled);
        Map<FieldDef, String> fields = this.definitionFields(draft);
        fields.put(STATUS, this.status(next));
        fields.put(GMT_TIME, Long.toString(System.currentTimeMillis()));
        if (previous == null) {
            fields.put(CREATE_TIME, fields.get(GMT_TIME));
        }

        List<DataMutation> changes = new ArrayList<>();
        changes.add(this.access.create(EntityType.INFO, previous == null ? OperationType.CREATE : OperationType.UPDATE, apiID, version, fields));
        if (!enabled) {
            this.disableReleases(changes, releases);
        }
        this.write(changes);
        return next;
    }

    private void checkVersion(Map<FieldDef, String> info, long version) {
        if ((info == null ? 0 : this.revision(info)) != version) {
            throw new DatawayException(409, "API changed; reload and retry");
        }
    }

    private ApiDefinition complete(ApiDefinition incoming, Map<FieldDef, String> previous) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(incoming.getId());
        definition.setMethod(incoming.getMethod());
        definition.setPath(incoming.getPath());
        definition.setType(incoming.getType());
        definition.setScript(incoming.getScript());
        definition.setDescription(incoming.getDescription());
        definition.setSchema(this.document(incoming.getSchema(), previous == null ? null : previous.get(SCHEMA)));
        definition.setSample(this.document(incoming.getSample(), previous == null ? null : previous.get(SAMPLE)));
        definition.setOptions(this.document(incoming.getOptions(), previous == null ? null : previous.get(OPTION)));
        return definition;
    }

    private String document(String incoming, String previous) {
        return incoming != null ? incoming : previous == null ? "{}" : previous;
    }

    private Map<FieldDef, String> definitionFields(ApiDefinition definition) {
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(METHOD, definition.getMethod());
        fields.put(PATH, definition.getPath());
        fields.put(TYPE, definition.getType() == ApiScriptType.SQL ? "SQL" : "DataQL");
        fields.put(SCRIPT, definition.getScript());
        fields.put(COMMENT, definition.getDescription());
        fields.put(SCHEMA, definition.getSchema());
        fields.put(SAMPLE, definition.getSample());
        fields.put(OPTION, definition.getOptions());
        return fields;
    }

    private String status(ApiState state) {
        if (!state.isPublished()) {
            return "0";
        }
        if (!state.isEnabled()) {
            return "3";
        }
        return state.isHasDraft() ? "2" : "1";
    }

    private void disableReleases(List<DataMutation> changes, List<Map<FieldDef, String>> releases) {
        for (var row : releases) {
            if ("1".equals(row.get(STATUS))) {
                changes.add(this.access.create(EntityType.RELEASE, OperationType.UPDATE, row.get(ID), this.revision(row), Map.of(STATUS, "3")));
            }
        }
    }

    private void write(List<DataMutation> changes) {
        try {
            this.access.write(changes);
        } catch (DataConflictException e) {
            throw new DatawayException(409, e.getMessage(), e);
        }
    }

    @Override
    public ApiState publish(String apiID, long version) {
        var info = this.info(apiID);
        this.checkVersion(info, version);
        var releases = this.releases(apiID);
        ApiDefinition draft = this.definition(info, false);
        long publishedAt = System.currentTimeMillis();
        if (!releases.isEmpty()) {
            publishedAt = Math.max(publishedAt, Long.parseLong(releases.get(releases.size() - 1).get(RELEASE_TIME)) + 1);
        }

        Map<FieldDef, String> snapshot = this.definitionFields(draft);
        snapshot.put(API_ID, apiID);
        snapshot.put(STATUS, "1");
        snapshot.put(RELEASE_TIME, Long.toString(publishedAt));
        List<DataMutation> changes = new ArrayList<>();
        changes.add(this.access.create(EntityType.INFO, OperationType.UPDATE, apiID, version, Map.of(STATUS, "1", GMT_TIME, Long.toString(System.currentTimeMillis()))));
        this.disableReleases(changes, releases);
        changes.add(this.access.create(EntityType.RELEASE, OperationType.CREATE, UUID.randomUUID().toString(), 0, snapshot));
        this.write(changes);
        return this.state(version + 1, draft, snapshot, true);
    }

    @Override
    public ApiState disableApi(String apiID, long version) {
        var info = this.info(apiID);
        this.checkVersion(info, version);
        var releases = this.releases(apiID);
        ApiState next = this.state(version + 1, this.definition(info, false), this.latest(releases), false);
        List<DataMutation> changes = new ArrayList<>();
        changes.add(this.access.create(EntityType.INFO, OperationType.UPDATE, apiID, version, Map.of(STATUS, this.status(next), GMT_TIME, Long.toString(System.currentTimeMillis()))));
        this.disableReleases(changes, releases);
        this.write(changes);
        return next;
    }

    @Override
    public void deleteApi(String apiID, long version) {
        this.checkVersion(this.info(apiID), version);
        List<DataMutation> changes = new ArrayList<>();
        changes.add(this.access.create(EntityType.INFO, OperationType.DELETE, apiID, version, Map.of()));
        for (var row : this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, apiID))) {
            changes.add(this.access.create(EntityType.RELEASE, OperationType.DELETE, row.get(ID), this.revision(row), Map.of()));
        }
        this.write(changes);
    }
}
