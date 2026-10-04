/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.model.ApiState;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.admin.AdminService;
import net.hasor.dataway.solon.DatawayPlugin;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;

/** Publishes the six executable examples accompanying the Dataway blog articles. */
@Component
public class BlogApiService {
    @Inject
    private Dataway      dataway;
    private AdminService admin;

    @Init(index = DatawayPlugin.INITIALIZATION_INDEX + 1)
    public void initialize() throws IOException {
        this.admin = this.dataway.getAdminService();
        for (String name : List.of("spring-query", "parameters", "pagination", "response-format", "swagger", "headers")) {
            this.publish(name);
        }
    }

    private void publish(String name) throws IOException {
        String path = "/blog/" + name;
        for (ApiDefinition existing : this.admin.list()) {
            if ("POST".equals(existing.getMethod()) && path.equals(existing.getPath())) {
                return;
            }
        }
        String resource = "/blog/" + name + "/";
        Map<String, ?> parameters = JsonUtils.readValue(this.read(resource + "parameters.json"), Map.class);
        Map<String, String> headers = "headers".equals(name) ? Map.of("Content-Type", "application/json", "X-Trace-Id", "blog-001") : Map.of("Content-Type", "application/json");
        ApiDefinition definition = new ApiDefinition();
        definition.setId("blog-" + name);
        definition.setMethod("POST");
        definition.setPath(path);
        definition.setDescription("Blog example: " + name);
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript(this.read(resource + "query.dql"));
        definition.setOptions(this.read(resource + "options.json"));
        definition.setSample(JsonUtils.writeValueAsString(Map.of("requestBody", parameters, "requestHeader", headers)));
        if ("swagger".equals(name)) {
            Map<String, Object> body = Map.of("type", "object", "properties", Map.of("message", Map.of("type", "string")));
            definition.setSchema(JsonUtils.writeValueAsString(Map.of("requestBody", body, "responseBody", body)));
        }
        ApiState saved = this.admin.save(definition, 0);
        this.admin.publish(saved.getApiID(), saved.getRevision());
    }

    private String read(String resource) throws IOException {
        try (InputStream input = BlogApiService.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
