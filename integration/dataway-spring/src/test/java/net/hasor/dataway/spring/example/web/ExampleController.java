/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example.web;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Supplies the example page with the host's configured entry paths. */
@RequestMapping("/example")
@RestController
public class ExampleController {
    @Autowired
    private Environment settings;

    @PostMapping("/config")
    public Map<String, ?> configuration(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String docs = this.entryPath(contextPath, "dataway.docs-prefix", "/docs");

        return Map.of(//
                "apiPrefix", this.entryPath(contextPath, "dataway.api-prefix", "/api"), //
                "admin", this.entryPath(contextPath, "dataway.admin-ui", "/admin") + "/",//
                "openapi", docs + "/openapi.json",  //
                "swagger", docs + "/swagger2.json", //
                "apiEnabled", this.settings.getProperty("dataway.api-enabled", Boolean.class, false),    //
                "adminEnabled", this.settings.getProperty("dataway.admin-enabled", Boolean.class, false),//
                "docsEnabled", this.settings.getProperty("dataway.docs-enabled", Boolean.class, false));
    }

    private String entryPath(String contextPath, String key, String defaultValue) {
        String prefix = this.settings.getProperty(key, defaultValue).trim().replaceAll("/+$", "");
        return contextPath + prefix;
    }
}
