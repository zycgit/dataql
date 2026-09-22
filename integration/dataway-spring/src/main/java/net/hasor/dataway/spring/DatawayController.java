/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.dataway.web.WebHandler;

/** MVC endpoint; the host's authentication and interceptors run before this method. */
public class DatawayController {
    private final String     prefix;
    private final WebHandler handler;

    public DatawayController(String prefix, WebHandler handler) {
        this.prefix = prefix;
        this.handler = handler;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        SpringWebRequest webRequest = new SpringWebRequest(request);
        String path = request.getRequestURI().substring(request.getContextPath().length());
        webRequest.setMethod(request.getMethod());
        webRequest.setPath(path);
        webRequest.setPathInfo(path.substring(this.prefix.length()));
        webRequest.setQuery(request.getQueryString());

        Map<String, List<String>> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, Collections.list(request.getHeaders(name))));
        webRequest.setHeaderValues(headers);

        Object identity = request.getAttribute(RequestAttribute.IDENTITY.getKey());
        if (identity instanceof UserIdentity user) {
            webRequest.setIdentity(user);
        } else {
            var principal = request.getUserPrincipal();
            if (principal != null) {
                webRequest.setIdentity(UserIdentity.authenticated(principal.getName()));
            }
        }

        SpringWebResponse webResponse = new SpringWebResponse(response);
        this.handler.handle(webRequest, webResponse);
    }
}
