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
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.MetadataContext;
import net.hasor.dataway.dal.MetadataProvider;
import java.util.List;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.web.WebHandler;
import org.noear.solon.core.*;

/**
 * Install with app.pluginAdd(0, new DatawayPlugin(builder)); reads the host Solon configuration.
 * Reads routing prefixes from Solon Props. Entry switches are owned by this integration.
 */
public final class DatawayPlugin implements Plugin {
    private final DatawayBuilder builder;
    private final Dataway        dataway;

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
            this.builder.defaultDataAccessLayer(() -> {
                var access = new DeferredDataAccessLayer(() -> MetadataLoader.create(new SolonMetadataContext(context)));
                context.lifecycle(access::initialize);
                return access;
            });
            dataway = this.builder.build();
        }
        context.wrapAndPut(Dataway.class, dataway);

        String apiPrefix = properties.get("api-prefix", "/api");
        String adminPrefix = properties.get("admin-prefix", "/dataway/api");
        String uiPrefix = properties.get("admin-ui", "/dataway");

        // API
        if (apiEnabled) {
            WebHandler apiHandler = dataway.getApiHandler();
            List<String> apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toList();
            register(context, apiPrefix, apiHandler, apiPaths);
        }

        // Admin API
        if (adminEnabled) {
            WebHandler adminHandler = dataway.getAdminHandler();
            List<String> adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toList();
            register(context, adminPrefix, adminHandler, adminPaths);

            // Admin UI
            WebHandler uiHandler = dataway.getUiHandler();
            List<String> uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toList();
            register(context, uiPrefix, uiHandler, uiPaths);
        }
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
