/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataway.authorization.AuthorizationCheck;
import net.hasor.dataway.authorization.DefaultAuthorizationCheck;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.service.admin.*;
import net.hasor.dataway.service.script.ApiInterceptor;
import net.hasor.dataway.service.script.DatawayApiHandler;
import net.hasor.dataway.service.script.DatawayEngine;
import net.hasor.dataway.web.body.UploadStorage;

/** Shared execution context with three independent HTTP handlers. The host owns their routing. */
public final class Dataway {
    private final AdminService adminService;
    private final WebHandler   apiHandler;
    private final WebHandler   adminHandler;
    private final WebHandler   adminUiHandler;

    public Dataway(DatawayConfig config) {
        ApiDataAccessLayer access = config.getDataAccessLayer();
        if (access == null) {
            throw new IllegalStateException("Supply an ApiDataAccessLayer before creating Dataway");
        }
        access.configureMapping(config.getTableMappings(), config.getFieldMappings());

        BeanContainer beans = new BeanContainer();

        // upload
        UploadStorage uploadStorage = new UploadStorage(config.getUploadTempDirectory(), config.getUploadMemoryThreshold());
        beans.setBean(UploadStorage.class, uploadStorage);
        beans.setBean(ApiDataAccessLayer.class, access); // dal
        beans.setBean(DatawayEngine.class, this.createEngine(config, beans));// engine

        // identity
        IdentityProvider identityProvider = config.getIdentityProvider();
        if (identityProvider == null) {
            identityProvider = WebRequest::getIdentity;
        }
        beans.setBean(IdentityProvider.class, identityProvider);

        // authorization
        AuthorizationCheck authorizationCheck = config.getAuthorizationCheck();
        if (authorizationCheck == null) {
            authorizationCheck = new DefaultAuthorizationCheck();
        }
        beans.setBean(AuthorizationCheck.class, authorizationCheck);

        // interceptor
        for (AdminInterceptor interceptor : config.getAdminInterceptors()) {
            beans.addBean(AdminInterceptor.class, interceptor);
        }

        // service
        this.adminService = new AdminServiceImpl(beans);
        beans.setBean(AdminService.class, this.adminService);

        // handler
        this.apiHandler = new DatawayApiHandler(beans);
        this.adminHandler = new DatawayAdminHandler(beans);
        this.adminUiHandler = new DatawayUiHandler(beans);
    }

    private DatawayEngine createEngine(DatawayConfig config, BeanContainer beans) {
        HostConfiguration host = new HostConfiguration(config.getFinder());
        config.getHostCustomizers().forEach(c -> c.accept(host));
        CustomizeScope scope = config.getCustomizeScope();
        if (scope == null) {
            scope = symbol -> Map.of();
        }

        beans.setBean(HostContext.class, host);
        beans.setBean(CustomizeScope.class, scope);
        for (ApiInterceptor interceptor : config.getApiInterceptors()) {
            beans.addBean(ApiInterceptor.class, interceptor);
        }

        // Later configuration changes must not affect engines already created.
        DatawayEngine engine = new DatawayEngine(beans, List.copyOf(config.getQueryCustomizers()));
        engine.setResponseFormat(config.getResponseFormat());
        engine.setResultStructure(config.isResultStructure());
        engine.setWrapAllParameters(config.isWrapAllParameters());
        engine.setWrapParameterName(config.getWrapParameterName());
        return engine;
    }

    /** Returns the shared API management service. */
    public AdminService getAdminService() {
        return this.adminService;
    }

    /** Returns the business API handler. */
    public WebHandler getApiHandler() {
        return this.apiHandler;
    }

    /** Returns the management API handler. */
    public WebHandler getAdminHandler() {
        return this.adminHandler;
    }

    /** Returns the management UI handler. */
    public WebHandler getAdminUiHandler() {
        return this.adminUiHandler;
    }
}
