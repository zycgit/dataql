/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.List;
import java.util.Objects;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.web.WebHandler;
import net.hasor.web.WebApiBinder;

/**
 * Registers the API, management API and UI independently in an existing Hasor application.
 * Reads routing prefixes from Hasor Settings, including hconfig.xml. Entry switches are owned by this integration.
 */
public final class DatawayModule implements Module {
    private static final String         CONFIG_PREFIX = "dataway.";
    private final        DatawayBuilder builder;
    private final        Dataway        dataway;

    public DatawayModule() {
        this(Dataway.builder());
    }

    public DatawayModule(DatawayBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        this.dataway = null;
    }

    public DatawayModule(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.builder = null;
    }

    @Override
    public void loadModule(ApiBinder binder) {
        Settings settings = binder.getSettings();
        boolean apiEnabled = settings.getBoolean(CONFIG_PREFIX + "api-enabled", false);
        boolean adminEnabled = settings.getBoolean(CONFIG_PREFIX + "admin-enabled", false);
        Dataway dataway = this.dataway;
        if (!apiEnabled && !adminEnabled && dataway == null) {
            return;
        }
        if (dataway == null) {
            dataway = this.builder.build();
        }
        binder.bindType(Dataway.class).toInstance(dataway);

        WebApiBinder web = binder.tryCast(WebApiBinder.class);
        if (web == null) {
            return; // Java-only Hasor hosts can use the assembled core directly.
        }

        String apiPrefix = settings.getString(CONFIG_PREFIX + "api-prefix", "/api");
        String adminPrefix = settings.getString(CONFIG_PREFIX + "admin-prefix", "/dataway/api");
        String uiPrefix = settings.getString(CONFIG_PREFIX + "admin-ui", "/dataway");

        // API
        if (apiEnabled) {
            WebHandler apiHandler = dataway.getApiHandler();
            List<String> apiPaths = apiHandler.paths().stream().map(path -> apiPrefix + path).toList();
            register(web, apiPrefix, apiHandler, apiPaths);
        }

        // Admin API
        if (adminEnabled) {
            WebHandler adminHandler = dataway.getAdminHandler();
            List<String> adminPaths = adminHandler.paths().stream().map(path -> adminPrefix + path).toList();
            register(web, adminPrefix, adminHandler, adminPaths);

            // Admin UI
            WebHandler uiHandler = dataway.getUiHandler();
            List<String> uiPaths = uiHandler.paths().stream().map(path -> uiPrefix + path).toList();
            register(web, uiPrefix, uiHandler, uiPaths);
        }
    }

    private void register(WebApiBinder web, String prefix, WebHandler handler, List<String> paths) {
        web.mappingTo(paths.toArray(String[]::new)).with(new DatawayController(prefix, handler));
    }
}
