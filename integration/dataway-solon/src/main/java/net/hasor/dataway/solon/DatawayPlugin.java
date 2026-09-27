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
import net.hasor.dataway.service.DatawayBuilder;
import net.hasor.dataway.service.WebHandler;
import org.noear.solon.core.*;

/**
 * Install with app.pluginAdd(0, new DatawayPlugin(builder)); reads the host Solon configuration.
 * Reads routing prefixes from Solon Props. Entry switches are owned by this integration.
 */
public final class DatawayPlugin implements Plugin {
    /** Order corresponding to Solon's @Init index; dependent initializers must use a larger index. */
    public static final int            INITIALIZATION_INDEX = 1;
    private final       DatawayBuilder builder;
    private final       Dataway        dataway;

    public DatawayPlugin() {
        this(Dataway.builder());
    }

    public DatawayPlugin(DatawayBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        this.dataway = null;
    }

    public DatawayPlugin(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.builder = null;
    }

    @Override
    public void start(AppContext context) {
        Props properties = context.app().cfg().getProp("dataway");
        boolean apiEnabled = properties.getBool("api-enabled", false);
        boolean adminEnabled = properties.getBool("admin-enabled", false);
        Dataway dataway = this.dataway;
        if (!apiEnabled && !adminEnabled && dataway == null) {
            return;
        }

        if (dataway == null) {
            // Solon schedules @Init(index = n) at lifecycle rank n + 1.
            context.lifecycle(INITIALIZATION_INDEX + 1, () -> {
                this.builder.defaultDataAccessLayer(() -> this.getDataAccessLayer(context));
                this.register(context, properties, this.builder.build(), apiEnabled, adminEnabled);
            });
            return;
        }

        this.register(context, properties, dataway, apiEnabled, adminEnabled);
    }

    private void register(AppContext context, Props properties, Dataway dataway, boolean apiEnabled, boolean adminEnabled) {
        context.wrapAndPut(Dataway.class, dataway);

        // API
        if (apiEnabled) {
            String apiPrefix = properties.get("api-prefix", "/api");
            WebHandler apiHandler = dataway.getApiHandler();
            List<String> apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toList();
            this.register(context, apiPrefix, apiHandler, apiPaths);
        }

        // Admin API
        if (adminEnabled) {
            String adminPrefix = properties.get("admin-prefix", "/dataway/api");
            WebHandler adminHandler = dataway.getAdminHandler();
            List<String> adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toList();
            this.register(context, adminPrefix, adminHandler, adminPaths);

            // Admin UI
            String uiPrefix = properties.get("admin-ui", "/dataway");
            WebHandler uiHandler = dataway.getUiHandler();
            List<String> uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toList();
            this.register(context, uiPrefix, uiHandler, uiPaths);
        }
    }

    private ApiDataAccessLayer getDataAccessLayer(AppContext context) {
        String name = context.app().cfg().getProperty("dataway.metadata.bean", "").trim();
        if (!name.isEmpty()) {
            Object bean = context.getBean(name);
            if (!(bean instanceof ApiDataAccessLayer access)) {
                throw new IllegalStateException("Missing or invalid ApiDataAccessLayer bean: " + name);
            }
            return access;
        }

        List<ApiDataAccessLayer> beans = context.getBeansOfType(ApiDataAccessLayer.class).stream().distinct().toList();
        if (beans.size() != 1) {
            throw new IllegalStateException("Expected one ApiDataAccessLayer bean; found " + beans.size() + "; configure dataway.metadata.bean");
        }

        return beans.getFirst();
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
