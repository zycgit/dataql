/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.web;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.Inject;
import net.hasor.web.annotation.MappingTo;
import net.hasor.web.annotation.Post;
import net.hasor.web.render.RenderType;

/** Supplies the example page with the host's configured entry paths. */
@MappingTo("/example")
@RenderType("json")
public class ExampleController {
    @Inject
    private Settings settings;

    @Post
    @MappingTo("/config")
    public Map<String, ?> configuration(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String docs = this.entryPath(contextPath, "dataway.docs-prefix", "/docs");

        return Map.of(//
                "apiPrefix", this.entryPath(contextPath, "dataway.api-prefix", "/api"), //
                "admin", this.entryPath(contextPath, "dataway.admin-ui", "/admin") + "/",//
                "openapi", docs + "/openapi.json",  //
                "swagger", docs + "/swagger2.json", //
                "apiEnabled", this.settings.getBoolean("dataway.api-enabled", false),    //
                "adminEnabled", this.settings.getBoolean("dataway.admin-enabled", false),//
                "docsEnabled", this.settings.getBoolean("dataway.docs-enabled", false));
    }

    private String entryPath(String contextPath, String key, String defaultValue) {
        String prefix = this.settings.getString(key, defaultValue).trim().replaceAll("/+$", "");
        return contextPath + prefix;
    }
}
