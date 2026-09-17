/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.web.*;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

/** Install with app.pluginAdd(0, new DatawayPlugin(service)); reads the host Solon configuration. */
public final class DatawayPlugin implements Plugin {
    private final DatawayBuilder builder;
    private final Dataway        dataway;

    public DatawayPlugin() {
        this(Dataway.builder());
    }

    public DatawayPlugin(DatawayService service) {
        this(Dataway.builder().service(service));
    }

    public DatawayPlugin(DatawayService service, WebOptions options) {
        this(Dataway.builder().service(service).webOptions(options));
    }

    public DatawayPlugin(WebOptions options) {
        this(Dataway.builder().webOptions(options));
    }

    public DatawayPlugin(DatawayBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        this.dataway = null;
    }

    public DatawayPlugin(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.builder = null;
    }

    @Override
    public void start(AppContext context) {
        Dataway dataway = this.dataway;
        if (dataway == null) {
            dataway = this.builder.configuration(context.app().cfg()::get).build();
        }
        context.wrapAndPut(Dataway.class, dataway);

        if (dataway.getService() != null) {
            context.wrapAndPut(DatawayService.class, dataway.getService());
        }

        for (WebHandler handler : dataway.getHandlers().values()) {
            this.register(context, handler);
        }
    }

    private void register(AppContext context, WebHandler handler) {
        context.app().router().filter(100, (request, chain) -> {
            String path = request.pathNew();
            if (!handler.matches(path)) {
                chain.doFilter(request);
                return;
            }

            Map<String, String> headers = new LinkedHashMap<>();
            request.headerNames().forEach(name -> headers.put(name, request.header(name)));
            String method = request.method();
            String queryString = request.queryString();
            var body = request.bodyAsStream();
            String principalName = request.attr(RequestAttribute.PRINCIPAL.getKey());

            WebRequest webRequest = new WebRequest(method, path, queryString, headers, body, principalName);
            WebResponse result = handler.handle(webRequest);
            request.status(result.status());
            result.headers().forEach(request::headerSet);
            request.output(result.body());
            request.setHandled(true);
        });
    }
}
