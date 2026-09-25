/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import net.hasor.dataway.authorization.UserIdentity;

/**
 * Paths retain percent escapes. path excludes the host context path; pathInfo also excludes
 * the entry's mount prefix. pathInfo is empty for the mount itself and starts with / below it.
 */
public abstract class WebRequest {
    private String                    method;
    private String                    path;
    private String                    pathInfo;
    private String                    query;
    private Map<String, String>       headers      = Map.of();
    private Map<String, List<String>> headerValues = Map.of();
    private UserIdentity              identity     = UserIdentity.anonymous();

    public void setMethod(String method) {
        this.method = Objects.requireNonNull(method).toUpperCase(Locale.ROOT);
    }

    public void setPath(String path) {
        this.path = Objects.requireNonNull(path);
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = Objects.requireNonNull(pathInfo);
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setHeaders(Map<String, String> headers) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        headers.forEach((name, value) -> values.put(name, List.of(value)));
        this.setHeaderValues(values);
    }

    public void setHeaderValues(Map<String, List<String>> headers) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        headers.forEach((name, entries) -> {
            String key = name.toLowerCase(Locale.ROOT);
            values.computeIfAbsent(key, ignored -> new ArrayList<>()).addAll(entries);
        });
        Map<String, String> first = new LinkedHashMap<>();
        values.replaceAll((name, entries) -> List.copyOf(entries));
        values.forEach((name, entries) -> {
            if (!entries.isEmpty()) {
                first.put(name, entries.getFirst());
            }
        });
        this.headerValues = Collections.unmodifiableMap(values);
        this.headers = Collections.unmodifiableMap(first);
    }

    public Map<String, List<String>> getHeaderValues() {
        return this.headerValues;
    }

    /** Cookie values remain encoded; repeated names are preserved in wire order. */
    public Map<String, List<String>> getCookies() {
        Map<String, List<String>> cookies = new LinkedHashMap<>();
        for (String line : this.headerValues.getOrDefault("cookie", List.of())) {
            for (String part : line.split(";")) {
                int equals = part.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String name = part.substring(0, equals).trim();
                String value = part.substring(equals + 1).trim();
                if (name.isEmpty() || name.startsWith("$")) {
                    continue;
                }
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                cookies.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            }
        }
        cookies.replaceAll((name, values) -> List.copyOf(values));
        return Collections.unmodifiableMap(cookies);
    }

    public String getMethod() {
        return this.method;
    }

    public String getPath() {
        return this.path;
    }

    public String getPathInfo() {
        return this.pathInfo;
    }

    public String getQuery() {
        return this.query;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /** Acquired lazily from the host request; Dataway never closes this stream. */
    public abstract InputStream getBody() throws IOException;

    /** Reads an attribute established by the host authentication/interceptor chain. */
    public abstract Object getAttribute(String name);

    public UserIdentity getIdentity() {
        return this.identity;
    }

    public void setIdentity(UserIdentity identity) {
        this.identity = Objects.requireNonNull(identity);
    }
}
