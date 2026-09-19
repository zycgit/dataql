/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.web.WebHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Registers three controller methods in the host MVC mapping. */
public final class DatawayMvcRegistrar implements SmartInitializingSingleton {
    private final Dataway dataway;
    private final boolean apiEnabled;
    private final boolean adminEnabled;
    private final String  apiPrefix;
    private final String  adminPrefix;
    private final String  uiPrefix;

    private final ObjectProvider<RequestMappingHandlerMapping> mappings;

    public DatawayMvcRegistrar(Dataway dataway, boolean apiEnabled, boolean adminEnabled, String apiPrefix, String adminPrefix, String uiPrefix, ObjectProvider<RequestMappingHandlerMapping> mappings) {
        this.mappings = mappings;
        this.dataway = apiEnabled || adminEnabled ? Objects.requireNonNull(dataway) : dataway;
        this.apiEnabled = apiEnabled;
        this.adminEnabled = adminEnabled;
        this.apiPrefix = apiPrefix;
        this.adminPrefix = adminPrefix;
        this.uiPrefix = uiPrefix;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // API
        if (this.apiEnabled) {
            WebHandler apiHandler = this.dataway.getApiHandler();
            String apiPrefix = this.apiPrefix;
            String[] apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toArray(String[]::new);
            register("datawayApi", apiHandler, apiPrefix, apiPaths);
        }

        // Admin API
        if (this.adminEnabled) {
            WebHandler adminHandler = this.dataway.getAdminHandler();
            String adminPrefix = this.adminPrefix;
            String[] adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toArray(String[]::new);
            register("datawayAdmin", adminHandler, adminPrefix, adminPaths);

            // Admin UI
            String uiPrefix = this.uiPrefix;
            WebHandler uiHandler = this.dataway.getUiHandler();
            String[] uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toArray(String[]::new);
            register("datawayUi", uiHandler, uiPrefix, uiPaths);
        }
    }

    private void register(String name, WebHandler handler, String prefix, String... paths) {
        RequestMappingHandlerMapping mapping = this.mappings.getObject();
        String[] patterns = java.util.Arrays.stream(paths).map(path -> {
            return path.endsWith("/*") ? path.substring(0, path.length() - 2) + "/{*path}" : path;
        }).toArray(String[]::new);

        var info = RequestMappingInfo.paths(patterns).mappingName(name).options(mapping.getBuilderConfiguration()).build();
        try {
            var method = DatawayController.class.getMethod("handle", HttpServletRequest.class, HttpServletResponse.class);
            mapping.registerMapping(info, new DatawayController(prefix, handler), method);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
