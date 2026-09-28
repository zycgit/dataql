/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.web.request.BodyReaders;
import net.hasor.dataway.web.request.UploadStorage;

/**
 * Paths retain percent escapes. path excludes the host context path; pathInfo also excludes
 * the entry's mount prefix. pathInfo is empty for the mount itself and starts with / below it.
 */
public abstract class WebRequest implements Closeable {
    private final List<Closeable>           resources     = new ArrayList<>();
    private       UploadStorage             uploadStorage = UploadStorage.DEFAULT;
    private       Map<String, Object>       body;
    private       String                    method;
    private       String                    path;
    private       String                    pathInfo;
    private       String                    query;
    private       Map<String, String>       headers       = Map.of();
    private       Map<String, List<String>> headerValues  = Map.of();
    private       UserIdentity              identity      = UserIdentity.anonymous();

    public void setMethod(String method) {
        this.method = method.toUpperCase(Locale.ROOT);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
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
        values.replaceAll((name, entries) -> Collections.unmodifiableList(entries));
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
        return cookies;
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

    /** Parses once; adapters may override this to reuse a body already parsed by the host. */
    public Map<String, Object> readBody() throws IOException {
        if (this.body == null) {
            this.body = BodyReaders.read(this);
        }
        return this.body;
    }

    /** Returns host-parsed form fields without query values, or null to read the raw form stream. */
    public Map<String, Object> getFormBody() throws IOException {
        return null;
    }

    /** Reads multipart fields and WebFile values using the host's upload facilities. */
    public Map<String, Object> getMultipartBody(Charset charset) throws IOException {
        throw new DatawayException(415, "Multipart parsing is not supported by this request adapter");
    }

    public void setUploadStorage(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    /** Caches and registers an upload without closing the supplied source stream. */
    protected WebFile cacheFile(String name, String contentType, InputStream input) throws IOException {
        WebFile file = this.uploadStorage.cache(name, contentType, input);
        this.registerResource(file);
        return file;
    }

    /** Registers host resources before reading, so partial failures still release them. */
    protected void registerResource(Closeable resource) {
        this.resources.add(resource);
    }

    /** Releases uploads after the response is written; the host request stream remains open. */
    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (Closeable resource : this.resources) {
            try {
                resource.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        this.resources.clear();
        if (failure != null) {
            throw failure;
        }
    }

    /** Acquired lazily from the host request; Dataway never closes this stream. */
    public abstract InputStream getBody() throws IOException;

    /** Reads an attribute established by the host authentication/interceptor chain. */
    public abstract Object getAttribute(String name);

    public UserIdentity getIdentity() {
        return this.identity;
    }

    public void setIdentity(UserIdentity identity) {
        this.identity = identity;
    }
}
