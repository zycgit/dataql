/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.admin;
import java.io.InputStream;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.ResultInfoUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.AbstractWebHandler;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;

/** Serves console files from the classpath under the host's configured UI prefix. */
public final class DatawayUiHandler extends AbstractWebHandler {
    private static final String              DEFAULT_UI_RESOURCE = "/META-INF/dataway-ui/";
    private static final Map<String, String> RESOURCE_HEADERS    = Map.of(//
            "X-Content-Type-Options", "nosniff",//
            "Cache-Control", "no-cache",        //
            "Content-Security-Policy", """
                    default-src 'self'; \
                    style-src 'self' 'unsafe-inline'; \
                    img-src 'self' data:; \
                    font-src 'self' data:; \
                    object-src 'none'; \
                    base-uri 'self'; \
                    frame-ancestors 'none'\
                    """);

    public DatawayUiHandler(Dataway dataway) {
        super(dataway);
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) {
        String method = request.getMethod();
        if (!StringUtils.equalsIgnoreCase(method, "GET") && !StringUtils.equalsIgnoreCase(method, "HEAD")) {
            throw new DatawayException(405, "Method not allowed");
        }

        String path = request.getPathInfo();
        if (path.isEmpty()) {
            String reqPath = request.getPath();
            ResultInfo result = new ResultInfo();
            result.setStatus(308);
            result.setData(new byte[0]);
            result.setJson(false);
            result.getHeaders().put("Location", reqPath.substring(reqPath.lastIndexOf('/') + 1) + "/");
            return result;
        }

        if (!path.startsWith("/") || path.contains("..") || path.contains("\\") || path.contains("%")) {
            throw new DatawayException(404, "Not found");
        }

        String name = path.equals("/") ? "index.html" : path.substring(1);
        InputStream stream = DatawayUiHandler.class.getResourceAsStream(DEFAULT_UI_RESOURCE + name);
        if (stream == null) {
            throw new DatawayException(404, "Asset not found");
        }

        ResultInfo result = ResultInfoUtils.ofStream(this.contentType(name), stream);
        result.getHeaders().putAll(RESOURCE_HEADERS);
        return result;
    }

    private String contentType(String name) {
        return switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "html" -> "text/html; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "json" -> "application/json; charset=utf-8";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "ttf" -> "font/ttf";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            default -> "application/octet-stream";
        };
    }
}
