/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.cobble.ResourcesUtils;
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

/** Publishes editable examples after Dataway and both business databases have initialized. */
@Component
public class ExampleApiService {
    @Inject
    private Dataway      dataway;
    private AdminService adminService;

    @Init(index = DatawayPlugin.INITIALIZATION_INDEX + 1)
    public void initialize() throws IOException {
        this.adminService = this.dataway.getAdminService();
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
        definition.setSample(JsonUtils.writeValueAsString(Map.of("requestBody", parameters, "requestHeader", "[]")));
        ApiState saved = this.adminService.save(definition, 0);
        this.adminService.publish(saved.getApiID(), saved.getRevision());
    }
}
