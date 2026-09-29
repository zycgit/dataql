/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.web;
import java.util.Map;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.AppContext;

@Controller
@Mapping("/example")
public class ExampleController {
    @Inject
    private AppContext context;

    @Post
    @Mapping("/config")
    public Map<String, ?> configuration() {
        var settings = this.context.cfg();
        String docs = this.entryPath("dataway.docs-prefix", "/docs");
        return Map.of("apiPrefix", this.entryPath("dataway.api-prefix", "/api"), "admin", this.entryPath("dataway.admin-ui", "/admin") + "/", "openapi", docs + "/openapi.json", "swagger", docs + "/swagger2.json", "apiEnabled", settings.getBool("dataway.api-enabled", false), "adminEnabled", settings.getBool("dataway.admin-enabled", false), "docsEnabled", settings.getBool("dataway.docs-enabled", false));
    }

    private String entryPath(String key, String defaultValue) {
        return this.context.cfg().get(key, defaultValue).trim().replaceAll("/+$", "");
    }
}
