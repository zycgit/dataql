/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.ConvertUtils;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.WebHandler;

/** Invokes published APIs; it exposes neither management operations nor UI assets. */
public final class DatawayApiHandler extends WebHandler {
    private static final String         SOURCE_HEADER = "x-dataway-source";
    private final        ApiServiceImpl apiService;

    public DatawayApiHandler(BeanContainer beans) {
        super(beans);
        this.apiService = beans.getBean(ApiServiceImpl.class);
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        UserIdentity identity = this.apiService.authorize(request.getIdentity());
        Map<String, Object> body = request.readBody();
        Map<String, Object> parameters = this.parameters(request, body);
        Map<String, ?> metadata = ConvertUtils.convertToWebContext(request, parameters, body);
        String marker = request.getHeaders().get(SOURCE_HEADER);
        ApiCallSource source = StringUtils.equalsIgnoreCase(marker, ApiCallSource.UI.name()) ? ApiCallSource.UI : ApiCallSource.HTTP;
        return this.apiService.invokePublished(request.getMethod(), request.getPathInfo(), parameters, identity, source, metadata, response);
    }

    private Map<String, Object> parameters(WebRequest request, Map<String, Object> body) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            for (String pair : request.getQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                String key = decode(parts[0]);
                String value = parts.length > 1 ? decode(parts[1]) : "";
                Object previous = values.get(key);
                if (previous == null) {
                    values.put(key, value);
                } else if (previous instanceof List<?> list) {
                    List<Object> repeated = new ArrayList<>(list);
                    repeated.add(value);
                    values.put(key, repeated);
                } else {
                    values.put(key, List.of(previous, value));
                }
            }
        }
        values.putAll(body);
        return values;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DatawayException(400, "Invalid request: " + e.getMessage(), e);
        }
    }
}
