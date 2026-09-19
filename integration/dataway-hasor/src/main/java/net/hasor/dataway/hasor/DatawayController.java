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
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.dataway.web.WebHandler;
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
        String path = request.getRequestURI().substring(request.getContextPath().length());
        webRequest.setMethod(request.getMethod());
        webRequest.setPath(path);
        webRequest.setPathInfo(path.substring(this.prefix.length()));
        webRequest.setQuery(request.getQueryString());

        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        webRequest.setHeaders(headers);

        Object identity = request.getAttribute(RequestAttribute.IDENTITY.getKey());
        if (identity instanceof UserIdentity user) {
            webRequest.setIdentity(user);
        } else {
            var principal = request.getUserPrincipal();
            if (principal != null) {
                webRequest.setIdentity(UserIdentity.authenticated(principal.getName()));
            }
        }

        HasorWebResponse webResponse = new HasorWebResponse(invoker.getHttpResponse());
        this.handler.handle(webRequest, webResponse);
    }
}
