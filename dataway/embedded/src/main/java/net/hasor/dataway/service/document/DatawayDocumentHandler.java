/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.DatawayException;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.WebHandler;

/** Serves API specifications without executing scripts or exposing management operations. */
public final class DatawayDocumentHandler extends WebHandler {
    private final DocumentService    documents;
    private final AuthorizationCheck authorizationCheck;

    public DatawayDocumentHandler(BeanContainer beans) {
        super(beans);
        this.documents = beans.getBean(DocumentService.class);
        this.authorizationCheck = beans.getBean(AuthorizationCheck.class);
    }

    @Override
    public List<String> paths() {
        return List.of("/swagger2.json", "/openapi.json");
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) {
        if (!this.paths().contains(request.getPathInfo())) {
            throw new DatawayException(404, "Document not found");
        }
        if (!StringUtils.equalsIgnoreCase(request.getMethod(), "GET") && !StringUtils.equalsIgnoreCase(request.getMethod(), "HEAD")) {
            throw new DatawayException(405, "Method not allowed");
        }
        if (!this.authorizationCheck.check(request.getIdentity(), Operation.DOCUMENT)) {
            throw new DatawayException(401, "Unauthorized");
        }

        Object document = switch (request.getPathInfo()) {
            case "/swagger2.json" -> this.documents.swagger2();
            default -> this.documents.openapi();
        };

        return ResultInfoUtils.json(200, document);
    }
}
