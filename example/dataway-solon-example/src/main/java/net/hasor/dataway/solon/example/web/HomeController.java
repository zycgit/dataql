/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.web;
import java.io.IOException;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;

/** The demo's three public resources, independent of the Dataway console resource handler. */
@Controller
public class HomeController {
    @Get
    @Mapping("/")
    public void index(Context context) throws IOException {
        this.resource(context, "index.html", "text/html; charset=UTF-8");
    }

    @Get
    @Mapping("/app.css")
    public void stylesheet(Context context) throws IOException {
        this.resource(context, "app.css", "text/css; charset=UTF-8");
    }

    @Get
    @Mapping("/app.js")
    public void javascript(Context context) throws IOException {
        this.resource(context, "app.js", "application/javascript; charset=UTF-8");
    }

    @Get
    @Mapping("/swagger/**")
    public void swagger(Context context) throws IOException {
        String name = context.path().substring("/swagger/".length());
        String type = switch (name) {
            case "swagger-ui-bundle.js.LICENSE.txt" -> "text/plain; charset=UTF-8";
            case "index.html" -> "text/html; charset=UTF-8";
            case "swagger-ui.css" -> "text/css; charset=UTF-8";
            case "swagger-ui-bundle.js", "initializer.js" -> "application/javascript; charset=UTF-8";
            default -> null;
        };
        if (type == null) {
            context.status(404);
            return;
        }
        this.resource(context, "swagger/" + name, type);
    }

    private void resource(Context context, String name, String contentType) throws IOException {
        try (var input = HomeController.class.getResourceAsStream("/web/" + name)) {
            if (input == null) {
                context.status(404);
                return;
            }
            context.contentType(contentType);
            context.headerSet("Cache-Control", "no-store");
            context.output(input);
        }
    }
}
