/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.service.DatawayService;
import net.hasor.dataway.web.WebHandler;
import net.hasor.dataway.web.WebOptions;
import net.hasor.dataway.web.WebRequest;
import net.hasor.dataway.web.WebResponse;
import net.hasor.web.WebApiBinder;

/** Registers the API, management API and UI independently in an existing Hasor application. */
public final class DatawayModule implements Module {
    private final DatawayBuilder builder;
    private final Dataway        dataway;

    public DatawayModule() {
        this(Dataway.builder());
    }

    public DatawayModule(DatawayService service) {
        this(Dataway.builder().service(service));
    }

    public DatawayModule(DatawayService service, WebOptions options) {
        this(Dataway.builder().service(service).webOptions(options));
    }

    public DatawayModule(WebOptions options) {
        this(Dataway.builder().webOptions(options));
    }

    public DatawayModule(DatawayBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        this.dataway = null;
    }

    public DatawayModule(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.builder = null;
    }

    @Override
    public void loadModule(ApiBinder binder) {
        Dataway dataway = this.dataway;
        if (dataway == null) {
            dataway = this.builder.configuration(key -> binder.getSettings().getString(key, null)).build();
        }
        binder.bindType(Dataway.class).toInstance(dataway);

        if (dataway.getService() != null) {
            binder.bindType(DatawayService.class).toInstance(dataway.getService());
        }

        WebApiBinder web = binder.tryCast(WebApiBinder.class);
        for (WebHandler handler : dataway.getHandlers().values()) {
            this.register(web, handler);
        }
    }

    private void register(WebApiBinder web, WebHandler handler) {
        if (web == null) {
            return; // Java-only Hasor hosts can use the assembled core directly.
        }

        web.filter(handler.pathPrefix(), handler.pathPrefix() + "/*").through((invoker, chain) -> {
            var request = invoker.getHttpRequest();
            var response = invoker.getHttpResponse();
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (!handler.matches(path)) {
                return chain.doNext(invoker);
            }

            Map<String, String> headers = new LinkedHashMap<>();
            Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
            String method = request.getMethod();
            String queryString = request.getQueryString();
            var body = request.getInputStream();
            var principal = request.getUserPrincipal();
            String principalName = principal == null ? null : principal.getName();

            WebRequest webRequest = new WebRequest(method, path, queryString, headers, body, principalName);
            WebResponse result = handler.handle(webRequest);
            response.setStatus(result.status());
            result.headers().forEach(response::setHeader);
            response.getOutputStream().write(result.body());
            return null;
        });
    }
}
