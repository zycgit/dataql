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
import java.util.List;
import java.util.Map;
import net.hasor.dataway.service.WebHandler;
import net.hasor.web.Invoker;
import net.hasor.web.annotation.Any;

/** MVC endpoint; the host's authentication and interceptors run before this method. */
public class DatawayController {
    private final String     prefix;
    private final WebHandler handler;

    public DatawayController(String prefix, WebHandler handler) {
        this.prefix = prefix;
        this.handler = handler;
    }

    @Any
    public void execute(Invoker invoker) throws Exception {
        var request = invoker.getHttpRequest();
        HasorWebRequest webRequest = new HasorWebRequest(request);
        HasorWebResponse webResponse = new HasorWebResponse(invoker.getHttpResponse());

        String path = request.getRequestURI().substring(request.getContextPath().length());
        webRequest.setMethod(request.getMethod());
        webRequest.setPath(path);
        webRequest.setPathInfo(path.substring(this.prefix.length()));
        webRequest.setQuery(request.getQueryString());

        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            headers.put(name, Collections.list(request.getHeaders(name)));
        });
        webRequest.setHeaderValues(headers);

        this.handler.handle(webRequest, webResponse);
    }
}
