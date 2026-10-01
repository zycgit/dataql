/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.Arrays;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.WebHandler;
import net.hasor.dataway.service.admin.DatawayUiHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Registers enabled Dataway entries in the host MVC mapping. */
public final class DatawayMvcRegistrar implements SmartInitializingSingleton {
    private final Dataway                                      dataway;
    private final boolean                                      apiEnabled;
    private final String                                       apiPrefix;
    private final boolean                                      adminEnabled;
    private final String                                       adminPrefix;
    private final String                                       adminUiPrefix;
    private final boolean                                      docsEnabled;
    private final String                                       docsPrefix;
    private final ObjectProvider<RequestMappingHandlerMapping> mappings;

    public DatawayMvcRegistrar(Dataway dataway, boolean apiEnabled, boolean adminEnabled, boolean docsEnabled,//
            String apiPrefix, String adminPrefix, String adminUiPrefix, String docsPrefix, ObjectProvider<RequestMappingHandlerMapping> mappings) {
        this.mappings = mappings;
        this.dataway = apiEnabled || adminEnabled || docsEnabled ? Objects.requireNonNull(dataway) : dataway;
        this.apiEnabled = apiEnabled;
        this.adminEnabled = adminEnabled;
        this.docsEnabled = docsEnabled;
        this.apiPrefix = apiPrefix;
        this.adminPrefix = adminPrefix;
        this.adminUiPrefix = adminUiPrefix;
        this.docsPrefix = docsPrefix;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // API
        if (this.apiEnabled) {
            WebHandler apiHandler = this.dataway.getApiHandler();
            String apiPrefix = this.apiPrefix;
            String[] apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toArray(String[]::new);
            this.register("datawayApi", apiHandler, apiPrefix, apiPaths);
        }

        // API specifications
        if (this.docsEnabled) {
            WebHandler docsHandler = this.dataway.getDocumentHandler();
            String docsPrefix = this.docsPrefix;
            String[] docsPaths = docsHandler.paths().stream().map(path -> docsPrefix + path).toArray(String[]::new);
            this.register("datawayDocs", docsHandler, docsPrefix, docsPaths);
        }

        // Admin API and UI
        if (this.adminEnabled) {
            WebHandler adminHandler = this.dataway.getAdminHandler();
            String adminPrefix = this.adminPrefix;
            String[] adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toArray(String[]::new);
            this.register("datawayAdmin", adminHandler, adminPrefix, adminPaths);

            // Register the UI wildcard after the API and document routes.
            String uiPrefix = this.adminUiPrefix;
            DatawayUiHandler uiHandler = (DatawayUiHandler) this.dataway.getAdminUiHandler();
            uiHandler.configureAddresses(uiPrefix, adminPrefix, this.apiEnabled ? this.apiPrefix : null);
            String[] uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toArray(String[]::new);
            this.register("datawayUi", uiHandler, uiPrefix, uiPaths);
        }
    }

    private void register(String name, WebHandler handler, String prefix, String... paths) {
        RequestMappingHandlerMapping mapping = this.mappings.getObject();
        String[] patterns = Arrays.stream(paths).map(path -> {
            return path.endsWith("/*") ? path.substring(0, path.length() - 2) + "/{*path}" : path;
        }).toArray(String[]::new);

        var info = RequestMappingInfo.paths(patterns).methods(RequestMethod.values()).mappingName(name).options(mapping.getBuilderConfiguration()).build();
        try {
            var method = DatawayController.class.getMethod("handle", HttpServletRequest.class, HttpServletResponse.class);
            mapping.registerMapping(info, new DatawayController(prefix, handler), method);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
