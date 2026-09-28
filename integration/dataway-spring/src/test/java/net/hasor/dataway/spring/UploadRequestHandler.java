/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebFile;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.BeanContainer;
import net.hasor.dataway.service.ResultInfoUtils;
import net.hasor.dataway.service.WebHandler;
import net.hasor.dataway.web.body.UploadStorage;

class UploadRequestHandler extends WebHandler {
    WebFile     file;
    IOException failure;

    UploadRequestHandler() {
        super(UploadRequestHandler.beans());
    }

    private static BeanContainer beans() {
        BeanContainer beans = new BeanContainer();
        beans.setBean(IdentityProvider.class, WebRequest::getIdentity);
        beans.setBean(UploadStorage.class, UploadStorage.DEFAULT);
        return beans;
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) throws Exception {
        this.file = (WebFile) request.readBody().get("file");
        if (this.failure != null) {
            throw this.failure;
        }
        return ResultInfoUtils.convertToResultInfo("application/octet-stream", this.file.openStream());
    }
}
