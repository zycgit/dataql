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
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.UdfParams;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.host.function.UdfName;
import net.hasor.dataway.model.WebCookie;
import net.hasor.dataway.model.WebResponse;

/** HTTP functions resolve resources from the current execution, never from framework globals. */
public class WebUdfSource extends AbstractUdfSource {
    /** Parsed request metadata supplied by the HTTP entry point. */
    public static final String HINT_REQUEST  = "DATAWAY_REQUEST";
    /** Response for the current HTTP invocation. */
    public static final String HINT_RESPONSE = "DATAWAY_RESPONSE";

    @Override
    public <T> T get(Class<? extends T> targetType) {
        return targetType.cast(this);
    }

    @Override
    public Predicate<Method> getPredicate(Class<?> targetType) {
        return m -> m.isAnnotationPresent(UdfName.class);
    }

    //
    // for header

    @UdfName("header")
    public Object header(String name, Hints hints) {
        return this.first(this.headerValues(hints), name);
    }

    @UdfName("headerArray")
    public List<?> headerArray(String name, Hints hints) {
        return this.values(this.headerValues(hints), name);
    }

    @UdfName("headerMap")
    public Map<String, Object> headerMap(Hints hints) {
        return this.firstValues(this.headerValues(hints));
    }

    @UdfName("headerArrayMap")
    public Map<?, ?> headerArrayMap(Hints hints) {
        return this.headerValues(hints);
    }

    private Map<?, ?> headerValues(Hints hints) {
        Object multiple = this.requestValue("headerValues", hints);
        Map<?, ?> source = multiple instanceof Map<?, ?> map ? map : this.map("headers", hints);
        Map<String, List<Object>> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.forEach((name, value) -> {
            if (name != null && value != null) {
                List<Object> items = values.computeIfAbsent(name.toString(), key -> new ArrayList<>());
                items.addAll(value instanceof List<?> list ? list : List.of(value));
            }
        });

        return values;
    }

    @UdfName("setHeader")
    public boolean setHeader(String name, Object value, Hints hints) {
        return this.writeHeader(name, value, false, hints);
    }

    @UdfName("addHeader")
    public boolean addHeader(String name, Object value, Hints hints) {
        return this.writeHeader(name, value, true, hints);
    }

    @UdfName("setHeaderAll")
    public boolean setHeaderAll(Map<?, ?> values, Hints hints) {
        return this.writeHeaders(values, false, hints);
    }

    private boolean writeHeader(String name, Object value, boolean append, Hints hints) {
        String headerValue = this.unwrap(value).toString();
        if (append) {
            this.response(hints).addHeader(name, headerValue);
        } else {
            this.response(hints).setHeader(name, headerValue);
        }
        return true;
    }

    private WebResponse response(Hints hints) {
        if (hints.getHint(HINT_RESPONSE) instanceof WebResponse response) {
            return response;
        } else {
            throw new IllegalStateException("HTTP response is unavailable in this execution");
        }
    }

    private boolean writeHeaders(Map<?, ?> values, boolean append, Hints hints) {
        for (var entry : values.entrySet()) {
            Object value = this.unwrap(entry.getValue());
            if (append && value instanceof List<?> list) {
                for (Object item : list) {
                    this.writeHeader(this.key(entry.getKey()), item, true, hints);
                }
            } else {
                this.writeHeader(this.key(entry.getKey()), value, append, hints);
            }
        }
        return true;
    }

    @UdfName("addHeaderAll")
    public boolean addHeaderAll(Map<?, ?> values, Hints hints) {
        return this.writeHeaders(values, true, hints);
    }

    //
    // for cookie

    @UdfName("cookie")
    public Object cookie(String name, Hints hints) {
        return this.first(this.map("cookies", hints), name);
    }

    @UdfName("cookieArray")
    public List<?> cookieArray(String name, Hints hints) {
        return this.values(this.map("cookies", hints), name);
    }

    @UdfName("cookieMap")
    public Map<String, Object> cookieMap(Hints hints) {
        return this.firstValues(this.map("cookies", hints));
    }

    @UdfName("cookieArrayMap")
    public Map<?, ?> cookieArrayMap(Hints hints) {
        return this.map("cookies", hints);
    }

    @UdfName("setCookie")
    public boolean setCookie(UdfParams params, Hints hints) {
        return this.writeCookie(params.allParams(), false, hints);
    }

    @UdfName("removeCookie")
    public boolean removeCookie(UdfParams params, Hints hints) {
        return this.writeCookie(params.allParams(), true, hints);
    }

    private boolean writeCookie(Object[] args, boolean remove, Hints hints) {
        int required = remove ? 1 : 2;
        if (args.length < required || args.length > required + 1) {
            throw new IllegalArgumentException("Cookie requires a name, a value when setting, and optional attributes");
        }

        String value = remove ? "" : this.argument(args, 1).toString();
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
        this.response(hints).setCookie(cookie);
        return true;
    }

    //
    // for body

    /** Returns the business body parsed by the HTTP entry point. */
    @UdfName("jsonBody")
    public Object jsonBody(Hints hints) {
        return this.requestValue("body", hints);
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

        return first;
    }

    private List<?> values(Map<?, ?> source, Object name) {
        String key = this.key(name);
        if (key == null) {
            return List.of();
        }

        if (source.containsKey(key)) {
            Object value = source.get(key);
            return value instanceof List<?> list ? list : List.of(value);
        }

        for (var entry : source.entrySet()) {
            String candidate = entry.getKey().toString();
            if (StringUtils.equalsIgnoreCase(key, candidate)) {
                Object value = entry.getValue();
                return value instanceof List<?> list ? list : List.of(value);
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

    private Map<?, ?> map(String name, Hints hints) {
        Object value = this.requestValue(name, hints);
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private Object requestValue(String name, Hints hints) {
        if (!(hints.getHint(HINT_REQUEST) instanceof Map<?, ?> metadata)) {
            return null;
        }

        if (metadata.containsKey(name)) {
            return metadata.get(name);
        }

        for (var entry : metadata.entrySet()) {
            if (entry.getKey() != null && StringUtils.equalsIgnoreCase(name, entry.getKey().toString())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private Object argument(Object[] args, int index) {
        return index < args.length ? this.unwrap(args[index]) : null;
    }
}
