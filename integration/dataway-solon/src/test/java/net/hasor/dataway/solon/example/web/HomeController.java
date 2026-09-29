/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.web;
import java.io.IOException;
import net.hasor.cobble.ResourcesUtils;
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

    private void resource(Context context, String name, String contentType) throws IOException {
        try (var input = ResourcesUtils.getResourceAsStream("/example/web/" + name)) {
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
