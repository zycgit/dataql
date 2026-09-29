/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.util.List;
import java.util.Objects;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.WebHandler;
import org.noear.solon.core.*;

/**
 * Install with app.pluginAdd(0, new DatawayPlugin(config)); reads the host Solon configuration.
 * Reads routing prefixes from Solon Props. Entry switches are owned by this integration.
 */
public final class DatawayPlugin implements Plugin {
    /** Initialization phase after ordinary dependency-based @Init ordering; consumers use a larger index. */
    public static final int           INITIALIZATION_INDEX = 100;
    private final       DatawayConfig config;
    private final       Dataway       dataway;

    public DatawayPlugin() {
        this(new DatawayConfig());
    }

    public DatawayPlugin(DatawayConfig config) {
        this.config = Objects.requireNonNull(config);
        this.dataway = null;
    }

    public DatawayPlugin(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.config = null;
    }

    @Override
    public void start(AppContext context) {
        Props properties = context.app().cfg().getProp("dataway");
        boolean apiEnabled = properties.getBool("api-enabled", false);
        boolean adminEnabled = properties.getBool("admin-enabled", false);
        boolean docsEnabled = properties.getBool("docs-enabled", false);
        Dataway dataway = this.dataway;
        if (!apiEnabled && !adminEnabled && !docsEnabled && dataway == null) {
            return;
        }

        if (dataway == null) {
            // Default @Init indices depend on injection depth; reserve a later phase for Dataway.
            // Solon schedules @Init(index = n) at lifecycle rank n + 1.
            context.lifecycle(INITIALIZATION_INDEX + 1, () -> {
                if (this.config.getDataAccessLayer() == null) {
                    this.config.dataAccessLayer(this.getDataAccessLayer(context));
                }
                this.register(context, properties, this.config.createDataway(), apiEnabled, adminEnabled, docsEnabled);
            });
            return;
        }

        this.register(context, properties, dataway, apiEnabled, adminEnabled, docsEnabled);
    }

    private void register(AppContext context, Props properties, Dataway dataway, boolean apiEnabled, boolean adminEnabled, boolean docsEnabled) {
        context.wrapAndPut(Dataway.class, dataway);

        // API
        if (apiEnabled) {
            String apiPrefix = properties.get("api-prefix", "/api");
            WebHandler apiHandler = dataway.getApiHandler();
            List<String> apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toList();
            this.register(context, apiPrefix, apiHandler, apiPaths);
        }

        // API specifications
        if (docsEnabled) {
            String docsPrefix = properties.get("docs-prefix", "/docs");
            WebHandler docsHandler = dataway.getDocumentHandler();
            List<String> docsPaths = docsHandler.paths().stream().map(path -> docsPrefix + path).toList();
            this.register(context, docsPrefix, docsHandler, docsPaths);
        }

        // Admin API and UI
        if (adminEnabled) {
            String adminPrefix = properties.get("admin-prefix", "/admin/api");
            WebHandler adminHandler = dataway.getAdminHandler();
            List<String> adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toList();
            this.register(context, adminPrefix, adminHandler, adminPaths);

            // Register the UI wildcard after the API and document routes.
            String uiPrefix = properties.get("admin-ui", "/admin");
            WebHandler uiHandler = dataway.getAdminUiHandler();
            List<String> uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toList();
            this.register(context, uiPrefix, uiHandler, uiPaths);
        }
    }

    private ApiDataAccessLayer getDataAccessLayer(AppContext context) {
        List<ApiDataAccessLayer> beans = context.getBeansOfType(ApiDataAccessLayer.class).stream().distinct().toList();
        if (beans.size() != 1) {
            throw new IllegalStateException("Expected one ApiDataAccessLayer bean; found " + beans.size());
        }

        return beans.get(0);
    }

    private void register(AppContext context, String prefix, WebHandler handler, List<String> paths) {
        var controller = new DatawayController(prefix, handler);
        var bean = new BeanWrap(context, DatawayController.class, controller);
        for (String path : paths) {
            String mapping = path.endsWith("/*") ? path + "*" : path;
            var loader = FactoryManager.getGlobal().createLoader(bean, false);
            loader.withPathPrefix(mapping).load((actionPath, method, index, action) -> {
                context.app().router().add(mapping, method, index, action);
            });
        }
    }
}
