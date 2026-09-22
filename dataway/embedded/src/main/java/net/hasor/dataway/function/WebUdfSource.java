/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.function;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.UdfSource;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.host.function.UdfName;
import net.hasor.dataql.host.function.UdfParams;
import net.hasor.dataway.web.WebCookie;
import net.hasor.dataway.web.WebResponse;

/** HTTP functions resolve resources from the current execution, never from framework globals. */
public class WebUdfSource extends AbstractUdfSource {
    private final Supplier<Map<String, ?>> request;
    private final Supplier<WebResponse>    response;

    public WebUdfSource(Supplier<Map<String, ?>> request, Supplier<WebResponse> response) {
        this.request = Objects.requireNonNull(request);
        this.response = Objects.requireNonNull(response);
    }

    /** ServiceLoader constructor; the host supplies the execution-bound instance. */
    public WebUdfSource() {
        this(Map::of, () -> null);
    }

    @Override
    public UdfSource create(HostContext context) {
        return context.getAttachment(WebUdfSource.class);
    }

    @Override
    public <T> T get(Class<? extends T> targetType) {
        return targetType.cast(this);
    }

    @Override
    public Predicate<Method> getPredicate(Class<?> targetType) {
        return m -> m.isAnnotationPresent(UdfName.class);
    }

    private WebResponse response() {
        WebResponse response = this.response.get();
        if (response == null) {
            throw new IllegalStateException("HTTP response is unavailable in this execution");
        }
        return response;
    }

    //
    // for header

    @UdfName("header")
    public Object header(String name) {
        return this.first(this.headerValues(), name);
    }

    @UdfName("headerArray")
    public List<?> headerArray(String name) {
        return this.values(this.headerValues(), name);
    }

    @UdfName("headerMap")
    public Map<String, Object> headerMap() {
        return this.firstValues(this.headerValues());
    }

    @UdfName("headerArrayMap")
    public Map<?, ?> headerArrayMap() {
        return this.headerValues();
    }

    private Map<?, ?> headerValues() {
        Object multiple = this.requestValue("headerValues");
        Map<?, ?> source = multiple instanceof Map<?, ?> map ? map : this.map("headers");
        Map<String, List<Object>> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.forEach((name, value) -> {
            if (name != null && value != null) {
                List<Object> items = values.computeIfAbsent(name.toString(), key -> new ArrayList<>());
                items.addAll(value instanceof List<?> list ? list : List.of(value));
            }
        });

        values.replaceAll((name, items) -> List.copyOf(items));
        return Collections.unmodifiableMap(values);
    }

    @UdfName("setHeader")
    public boolean setHeader(String name, Object value) {
        return this.writeHeader(name, value, false);
    }

    @UdfName("addHeader")
    public boolean addHeader(String name, Object value) {
        return this.writeHeader(name, value, true);
    }

    @UdfName("setHeaderAll")
    public boolean setHeaderAll(Map<?, ?> values) {
        return this.writeHeaders(values, false);
    }

    private boolean writeHeader(String name, Object value, boolean append) {
        String headerValue = Objects.requireNonNull(this.unwrap(value), "header value").toString();
        if (append) {
            this.response().addHeader(name, headerValue);
        } else {
            this.response().setHeader(name, headerValue);
        }
        return true;
    }

    private boolean writeHeaders(Map<?, ?> values, boolean append) {
        Objects.requireNonNull(values, "header map");
        for (var entry : values.entrySet()) {
            Object value = this.unwrap(entry.getValue());
            if (append && value instanceof List<?> list) {
                for (Object item : list) {
                    this.writeHeader(this.key(entry.getKey()), item, true);
                }
            } else {
                this.writeHeader(this.key(entry.getKey()), value, append);
            }
        }
        return true;
    }

    @UdfName("addHeaderAll")
    public boolean addHeaderAll(Map<?, ?> values) {
        return this.writeHeaders(values, true);
    }

    //
    // for cookie

    @UdfName("cookie")
    public Object cookie(String name) {
        return this.first(this.map("cookies"), name);
    }

    @UdfName("cookieArray")
    public List<?> cookieArray(String name) {
        return this.values(this.map("cookies"), name);
    }

    @UdfName("cookieMap")
    public Map<String, Object> cookieMap() {
        return this.firstValues(this.map("cookies"));
    }

    @UdfName("cookieArrayMap")
    public Map<?, ?> cookieArrayMap() {
        return this.map("cookies");
    }

    @UdfName("setCookie")
    public boolean setCookie(UdfParams params) {
        return this.writeCookie(params.allParams(), false);
    }

    @UdfName("removeCookie")
    public boolean removeCookie(UdfParams params) {
        return this.writeCookie(params.allParams(), true);
    }

    private boolean writeCookie(Object[] args, boolean remove) {
        int required = remove ? 1 : 2;
        if (args.length < required || args.length > required + 1) {
            throw new IllegalArgumentException("Cookie requires a name, a value when setting, and optional attributes");
        }

        String value = remove ? "" : Objects.requireNonNull(this.argument(args, 1), "cookie value").toString();
        WebCookie cookie = new WebCookie(this.key(args[0]), value);
        Object attributes = this.argument(args, required);
        if (attributes != null) {
            if (!(attributes instanceof Map<?, ?> options)) {
                throw new IllegalArgumentException("Cookie attributes must be a map");
            }

            for (var entry : options.entrySet()) {
                Object option = this.unwrap(entry.getValue());
                switch (entry.getKey().toString().toLowerCase(Locale.ROOT)) {
                    case "path" -> cookie.setPath(option == null ? null : option.toString());
                    case "domain" -> cookie.setDomain(option == null ? null : option.toString());
                    case "maxage" -> cookie.setMaxAge(option == null ? null : Long.valueOf(option.toString()));
                    case "secure" -> cookie.setSecure((Boolean) option);
                    case "httponly" -> cookie.setHttpOnly((Boolean) option);
                    case "samesite" -> cookie.setSameSite(option == null ? null : option.toString());
                    default -> throw new IllegalArgumentException("Unknown cookie attribute: " + entry.getKey());
                }
            }
        }

        if (remove) {
            cookie.setMaxAge(0L);
        }
        this.response().setCookie(cookie);
        return true;
    }

    //
    // for body

    /** Parsed business body; management debug uses its parameters as the simulated body. */
    @UdfName("jsonBody")
    public Object jsonBody() {
        return this.requestValue("body");
    }

    //

    private Object first(Map<?, ?> source, Object name) {
        List<?> values = this.values(source, name);
        return values.isEmpty() ? null : values.getFirst();
    }

    private Map<String, Object> firstValues(Map<?, ?> source) {
        Map<String, Object> first = new LinkedHashMap<>();
        source.forEach((name, value) -> {
            List<?> values = value instanceof List<?> list ? list : List.of(value);
            if (!values.isEmpty()) {
                first.put(name.toString(), values.getFirst());
            }
        });

        return Collections.unmodifiableMap(first);
    }

    private List<?> values(Map<?, ?> source, Object name) {
        String key = this.key(name);
        if (key == null) {
            return List.of();
        }

        if (source.containsKey(key)) {
            Object value = source.get(key);
            return value instanceof List<?> list ? List.copyOf(list) : List.of(value);
        }

        for (var entry : source.entrySet()) {
            String candidate = entry.getKey().toString();
            if (key.equalsIgnoreCase(candidate)) {
                Object value = entry.getValue();
                return value instanceof List<?> list ? List.copyOf(list) : List.of(value);
            }
        }

        return List.of();
    }

    private String key(Object value) {
        Object unwrapped = this.unwrap(value);
        return unwrapped == null || unwrapped.toString().isBlank() ? null : unwrapped.toString();
    }

    private Object unwrap(Object value) {
        return value instanceof DataModel model ? model.unwrap() : value;
    }

    private Map<?, ?> map(String name) {
        Object value = this.requestValue(name);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private Object requestValue(String name) {
        Map<String, ?> metadata = this.request.get();
        if (metadata.containsKey(name)) {
            return metadata.get(name);
        }

        for (var entry : metadata.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private Object argument(Object[] args, int index) {
        return index < args.length ? this.unwrap(args[index]) : null;
    }
}