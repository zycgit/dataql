/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.config;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.WebHandler;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.mockito.Mockito.*;

/** Shared fixtures; storage is mocked, service and HTTP entry behavior remain real. */
public abstract class ServiceTestSupport {
    protected final ApiDataAccessLayer access = mock(ApiDataAccessLayer.class, CALLS_REAL_METHODS);
    protected final DatawayConfig      config = new DatawayConfig().dataAccessLayer(this.access);

    protected MemoryRequest request(String method, String path) {
        MemoryRequest request = new MemoryRequest();
        request.setMethod(method);
        request.setPath("/mounted" + path);
        request.setPathInfo(path);
        return request;
    }

    protected MemoryResponse handle(WebHandler handler, String method, String path) throws Exception {
        MemoryResponse response = new MemoryResponse();
        handler.handle(this.request(method, path), response);
        return response;
    }

    protected Map<FieldDef, String> info(String id, String status, long revision) {
        Map<FieldDef, String> row = new EnumMap<>(FieldDef.class);
        row.put(ID, id);
        row.put(STATUS, status);
        row.put(REVISION, Long.toString(revision));
        row.put(METHOD, "GET");
        row.put(PATH, "/" + id);
        row.put(TYPE, "DataQL");
        row.put(SCRIPT, "return 'value';");
        row.put(COMMENT, "Example API");
        row.put(SCHEMA, "{}");
        row.put(SAMPLE, "{}");
        row.put(OPTION, "{}");
        row.put(CREATE_TIME, "1");
        row.put(GMT_TIME, "2");
        return row;
    }

    protected Map<FieldDef, String> release(Map<FieldDef, String> info, String id, String status, long time) {
        Map<FieldDef, String> row = new EnumMap<>(info);
        row.put(API_ID, info.get(ID));
        row.put(ID, id);
        row.put(STATUS, status);
        row.put(RELEASE_TIME, Long.toString(time));
        row.put(REVISION, "1");
        return row;
    }

    protected void storeInfo(Map<FieldDef, String> info) {
        doReturn(Optional.of(info)).when(this.access).getObject(EntityType.INFO, info.get(ID));
    }

    protected void storeReleases(String apiID, List<Map<FieldDef, String>> releases) {
        when(this.access.listObjects(EntityType.RELEASE, Map.of(API_ID, apiID))).thenReturn(releases);
    }

    protected void publishRoute(Map<FieldDef, String> release) {
        Map<FieldDef, String> filter = Map.of(METHOD, release.get(METHOD), PATH, release.get(PATH), STATUS, "1");
        when(this.access.listObjects(EntityType.RELEASE, filter)).thenReturn(List.of(release));
    }

    protected ApiDefinition definition(String id, String script) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("GET");
        definition.setPath("/" + id);
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(script);
        definition.setDescription("Example API");
        return definition;
    }

    protected MemoryResponse invokeScript(String script) throws Exception {
        Map<FieldDef, String> info = this.info("test", "1", 1);
        info.put(SCRIPT, script);
        this.publishRoute(this.release(info, "release", "1", 1));
        Dataway dataway = this.config.createDataway();
        return this.handle(dataway.getApiHandler(), "GET", "/test");
    }
}
