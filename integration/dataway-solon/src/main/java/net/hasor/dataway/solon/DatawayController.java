/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.LinkedHashMap;
import java.util.Map;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.web.RequestAttribute;
import net.hasor.dataway.web.WebHandler;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;

public class DatawayController {
    private final String     prefix;
    private final WebHandler handler;

    public DatawayController(String prefix, WebHandler handler) {
        this.prefix = prefix;
        this.handler = handler;
    }

    @Mapping("")
    public void handle(Context context) throws Exception {
        SolonWebRequest webRequest = new SolonWebRequest(context);
        String path = context.pathNew();
        webRequest.setMethod(context.method());
        webRequest.setPath(path);
        webRequest.setPathInfo(path.substring(this.prefix.length()));
        webRequest.setQuery(context.queryString());

        Map<String, String> headers = new LinkedHashMap<>();
        context.headerNames().forEach(name -> headers.put(name, context.header(name)));
        webRequest.setHeaders(headers);

        UserIdentity identity = context.attr(RequestAttribute.IDENTITY.getKey());
        if (identity != null) {
            webRequest.setIdentity(identity);
        }

        SolonWebResponse webResponse = new SolonWebResponse(context);
        this.handler.handle(webRequest, webResponse);
    }
}
