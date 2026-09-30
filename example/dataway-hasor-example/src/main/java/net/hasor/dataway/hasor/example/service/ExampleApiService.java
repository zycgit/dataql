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
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.admin.AdminService;

/** Publishes editable examples after Dataway and both business databases have initialized. */
public class ExampleApiService {
    private final AdminService adminService;

    public ExampleApiService(AdminService adminService) {
        this.adminService = adminService;
    }

    public void initialize() throws IOException {
        this.publish("echo", ApiScriptType.DATA_QL, "Echo a JSON request", Map.of("message", "Hello Dataway"));
        this.publish("form", ApiScriptType.DATA_QL, "Submit URL-encoded form fields", Map.of("name", "Dataway", "tag", List.of("one", "two")));
        this.publish("upload", ApiScriptType.DATA_QL, "Inspect a multipart upload", Map.of("title", "Example upload"));
        this.publish("person", ApiScriptType.DATA_QL, "Query one person from data source ds1", Map.of("id", 1));
        this.publish("people", ApiScriptType.DATA_QL, "List people from data source ds1", Map.of());
        this.publish("orders", ApiScriptType.DATA_QL, "Query a person's orders from data source ds2", Map.of("id", 1));
        this.publish("person-orders", ApiScriptType.DATA_QL, "Combine a person from ds1 with their orders from ds2", Map.of("id", 1));
    }

    private void publish(String name, ApiScriptType type, String description, Map<String, ?> parameters) throws IOException {
        String path = "/" + name;
        for (ApiDefinition existing : this.adminService.list()) {
            if ("POST".equals(existing.getMethod()) && path.equals(existing.getPath())) {
                return;
            }
        }

        String extension = type == ApiScriptType.SQL ? ".sql" : ".dql";
        String resource = "/dataway/" + name + extension;
        String script;
        try (var input = ExampleApiService.class.getResourceAsStream(resource)) {
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
        String contentType = switch (name) {
            case "form" -> "application/x-www-form-urlencoded";
            case "upload" -> "multipart/form-data";
            default -> "application/json";
        };
        definition.setSample(JsonUtils.writeValueAsString(Map.of("requestBody", parameters,
                "requestHeader", Map.of("Content-Type", contentType))));
        if ("upload".equals(name)) {
            Map<String, Object> bodySchema = Map.of("type", "object", "required", List.of("file"),
                    "properties", Map.of("title", Map.of("type", "string"),
                            "file", Map.of("type", "string", "format", "binary")));
            definition.setSchema(JsonUtils.writeValueAsString(Map.of("requestBody", bodySchema)));
        }
        ApiState saved = this.adminService.save(definition, 0);
        this.awaitVersion(saved.getApiID(), saved.getRevision());
        ApiState published = this.adminService.publish(saved.getApiID(), saved.getRevision());
        this.awaitVersion(published.getApiID(), published.getRevision());
    }

    // Nacos publication and query visibility are asynchronous; never repeat the write blindly.
    private void awaitVersion(String apiID, long version) throws IOException {
        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                if (this.adminService.getVersionById(apiID) >= version) {
                    return;
                }
            } catch (DatawayException error) {
                if (error.status() != 404) {
                    throw error;
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while initializing example APIs", e);
            }
        }
        throw new IOException("API metadata version is not visible: " + apiID + " / " + version);
    }
}
