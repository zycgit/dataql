/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.net.URI;
import java.util.*;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import static net.hasor.dataway.dal.FieldDef.*;

/** Describes currently active releases using stored metadata, independently of script execution. */
public class DocumentService {
    private final ApiDataAccessLayer access;
    private final String             title;
    private final String             version;
    private final URI                server;

    public DocumentService(BeanContainer beans, String title, String version, String server) {
        this.access = beans.getBean(ApiDataAccessLayer.class);
        this.title = this.required(title, "documentTitle");
        this.version = this.required(version, "documentVersion");
        this.server = URI.create(this.required(server, "documentServer"));
        boolean absolute = this.server.isAbsolute();
        boolean http = "http".equalsIgnoreCase(this.server.getScheme()) || "https".equalsIgnoreCase(this.server.getScheme());
        boolean relative = !absolute && server.startsWith("/") && this.server.getRawAuthority() == null;
        boolean validLocation = relative || (http && this.server.getHost() != null);
        boolean extraComponents = this.server.getRawQuery() != null || this.server.getRawFragment() != null || this.server.getUserInfo() != null;
        if (!validLocation || extraComponents) {
            throw new IllegalArgumentException("documentServer must be an HTTP(S) URL or a root-relative path without query, fragment or credentials");
        }
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public Map<String, Object> swagger2() {
        return this.document(true);
    }

    public Map<String, Object> openapi() {
        return this.document(false);
    }

    private Map<String, Object> document(boolean swagger) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(swagger ? "swagger" : "openapi", swagger ? "2.0" : "3.2.1");
        result.put("info", Map.of("title", this.title, "version", this.version));
        if (swagger) {
            if (this.server.isAbsolute()) {
                result.put("host", this.server.getRawAuthority());
                result.put("schemes", List.of(this.server.getScheme().toLowerCase(Locale.ROOT)));
            }
            String path = this.server.getRawPath();
            result.put("basePath", path == null || path.isEmpty() ? "/" : path);
        } else {
            result.put("servers", List.of(Map.of("url", this.server.toString())));
        }

        List<Map<FieldDef, String>> releases = new ArrayList<>(this.access.listObjects(EntityType.RELEASE, Map.of(STATUS, "1")));
        Comparator<Map<FieldDef, String>> order = Comparator.comparingLong(row -> Long.parseLong(row.get(RELEASE_TIME)));
        releases.sort(order.reversed().thenComparing(row -> row.get(ID)));
        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();
        Map<String, Object> schemas = new LinkedHashMap<>();
        for (Map<FieldDef, String> release : releases) {
            String method = release.get(METHOD).toLowerCase(Locale.ROOT);
            String path = release.get(PATH);
            Map<String, Object> operations = paths.computeIfAbsent(path, key -> new LinkedHashMap<>());
            if (operations.containsKey(method)) {
                continue;
            }
            if (!Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace").contains(method) || (swagger && "trace".equals(method))) {
                throw new DatawayException(422, "HTTP method cannot be represented in this document: " + method);
            }
            operations.put(method, this.operation(release, method, swagger, schemas));
        }
        result.put("paths", paths);
        if (!schemas.isEmpty()) {
            result.put(swagger ? "definitions" : "components", swagger ? schemas : Map.of("schemas", schemas));
        }
        return result;
    }

    private Map<String, Object> operation(Map<FieldDef, String> release, String method, boolean swagger, Map<String, Object> schemas) {
        DocumentSchema metadata = new DocumentSchema(release.get(SCHEMA), release.get(SAMPLE));
        metadata.registerSchemas(release.get(API_ID), schemas, swagger);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", "api_" + release.get(API_ID) + "_" + method);
        result.put("summary", release.getOrDefault(COMMENT, ""));
        List<Map<String, Object>> parameters = metadata.parameters("requestHeader", "header", swagger);
        boolean query = StringUtils.equalsIgnoreCase(method, "GET") || StringUtils.equalsIgnoreCase(method, "HEAD");
        String requestType = metadata.contentType("requestHeader");
        if (query) {
            parameters.addAll(metadata.parameters("requestBody", "query", swagger));
        } else if (metadata.has("requestBody")) {
            if (swagger) {
                if ("multipart/form-data".equals(requestType) || "application/x-www-form-urlencoded".equals(requestType)) {
                    parameters.addAll(metadata.parameters("requestBody", "formData", true));
                } else {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("in", "body");
                    body.put("name", "body");
                    Map<String, Object> schema = metadata.schema("requestBody", true);
                    Map<String, Object> bodySchema = new LinkedHashMap<>(schema);
                    if (metadata.hasSample("requestBody")) {
                        if (schema.containsKey("$ref")) {
                            bodySchema = new LinkedHashMap<>();
                            bodySchema.put("allOf", List.of(schema));
                        }
                        bodySchema.put("example", metadata.sample("requestBody"));
                    }
                    body.put("schema", bodySchema);
                    parameters.add(body);
                }
                result.put("consumes", List.of(requestType));
            } else {
                result.put("requestBody", Map.of("content", Map.of(requestType, metadata.content("requestBody"))));
            }
        }
        if (!parameters.isEmpty()) {
            result.put("parameters", parameters);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "OK");
        String responseType = metadata.contentType("responseHeader");
        if (swagger) {
            result.put("produces", List.of(responseType));
            if (metadata.has("responseBody")) {
                response.put("schema", metadata.schema("responseBody", true));
                if (metadata.hasSample("responseBody")) {
                    Map<String, Object> examples = new LinkedHashMap<>();
                    examples.put(responseType, metadata.sample("responseBody"));
                    response.put("examples", examples);
                }
            }
        } else if (metadata.has("responseBody")) {
            response.put("content", Map.of(responseType, metadata.content("responseBody")));
        }
        Map<String, Object> headers = metadata.responseHeaders(swagger);
        if (!headers.isEmpty()) {
            response.put("headers", headers);
        }
        result.put("responses", Map.of("200", response));
        return result;
    }
}
