/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.admin.AdminService;

/** Publishes editable examples after Dataway and both business databases have initialized. */
public class ExampleApiService {
    private final AdminService adminService;

    public ExampleApiService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void initialize() throws IOException {
        this.publish("person", ApiScriptType.DATA_QL, "Query one person from data source ds1", Map.of("id", 1));
        this.publish("people", ApiScriptType.DATA_QL, "List people from data source ds1", Map.of());
        this.publish("orders", ApiScriptType.DATA_QL, "Query a person's orders from data source ds2", Map.of("id", 1));
        this.publish("person-orders", ApiScriptType.DATA_QL, "Combine a person from ds1 with their orders from ds2", Map.of("id", 1));
        this.publish("result-structure", ApiScriptType.DATA_QL, "Return a structured JSON response", Map.of("message", "Hello Dataway"));
        this.publish("result-raw", ApiScriptType.DATA_QL, "Return the original JSON value", Map.of("message", "Hello Dataway"));
        this.publish("result-text", ApiScriptType.DATA_QL, "Return plain text", Map.of("message", "Hello Dataway"));
        this.publish("verifyCode", ApiScriptType.DATA_QL, "Render script text as a PNG verification code", Map.of("text", "A7K9"));
        this.publish("people-csv", ApiScriptType.DATA_QL, "Export people as CSV using a result handler", Map.of());
        this.publish("upload-download", ApiScriptType.DATA_QL, "Return an uploaded file unchanged", Map.of());
        this.publish("binary", ApiScriptType.DATA_QL, "Return UTF-8 binary content from a Web UDF", Map.of());
    }

    private void publish(String name, ApiScriptType type, String description, Map<String, ?> parameters) throws IOException {
        String path = "/" + name;
        for (ApiDefinition existing : this.adminService.list()) {
            if ("POST".equals(existing.getMethod()) && path.equals(existing.getPath())) {
                return;
            }
        }

        String extension = type == ApiScriptType.SQL ? ".sql" : ".dql";
        String resource = "/example/dataway/" + name + extension;
        String script;
        try (var input = ResourcesUtils.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Example script not found: " + resource);
            }
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        ApiDefinition definition = new ApiDefinition();
        definition.setId("example-" + name);
        definition.setMethod("POST");
        definition.setPath(path);
        definition.setType(type);
        definition.setScript(script);
        definition.setDescription(description);
        String handler = switch (name) {
            case "result-structure" -> "structure";
            case "result-raw" -> "raw";
            case "result-text" -> "text";
            case "people-csv" -> "csv";
            case "verifyCode" -> "verifyCode";
            default -> null;
        };
        if (handler != null) {
            definition.setOptions(JsonUtils.writeValueAsString(Map.of("resultHandler", handler)));
        }
        definition.setSample(JsonUtils.writeValueAsString(Map.of("requestBody", parameters, "requestHeader", "[]")));
        if (List.of("people-csv", "upload-download", "binary", "result-text", "verifyCode").contains(name)) {
            Map<String, Object> schema = new LinkedHashMap<>();
            String requestType = "application/json";
            if ("upload-download".equals(name)) {
                requestType = "multipart/form-data";
                schema.put("requestBody", Map.of("type", "object", "required", List.of("file"), "properties", Map.of("file", Map.of("type", "string", "format", "binary"))));
            }
            String responseType = switch (name) {
                case "people-csv" -> "text/csv";
                case "result-text" -> "text/plain";
                case "verifyCode" -> "image/png";
                default -> "application/octet-stream";
            };
            if ("result-text".equals(name)) {
                schema.put("responseBody", Map.of("type", "string"));
            } else {
                schema.put("responseBody", Map.of("type", "string", "format", "binary"));
            }
            definition.setSchema(JsonUtils.writeValueAsString(schema));
            definition.setSample(JsonUtils.writeValueAsString(Map.of("requestBody", parameters, "requestHeader", Map.of("Content-Type", requestType), "responseHeader", Map.of("Content-Type", responseType))));
        }
        ApiState saved = this.adminService.save(definition, 0);
        this.adminService.publish(saved.getApiID(), saved.getRevision());
    }
}
