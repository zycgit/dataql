/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.web.WebHandler;
import net.hasor.dataway.web.WebRequest;
import net.hasor.dataway.web.WebResponse;

public final class DatawayFilter implements Filter {
    private final WebHandler handler;

    public DatawayFilter(WebHandler handler) {
        this.handler = handler;
    }

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) input;
        HttpServletResponse response = (HttpServletResponse) output;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!this.handler.matches(path)) {
            chain.doFilter(input, output);
            return;
        }

        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
        String method = request.getMethod();
        String queryString = request.getQueryString();
        var body = request.getInputStream();
        var principal = request.getUserPrincipal();
        String principalName = principal == null ? null : principal.getName();

        WebRequest webRequest = new WebRequest(method, path, queryString, headers, body, principalName);
        WebResponse result = this.handler.handle(webRequest);
        response.setStatus(result.status());
        result.headers().forEach(response::setHeader);
        response.getOutputStream().write(result.body());
    }
}
