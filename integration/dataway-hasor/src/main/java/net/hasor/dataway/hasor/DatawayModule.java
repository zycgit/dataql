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
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import net.hasor.cobble.provider.Provider;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.WebHandler;
import net.hasor.web.WebApiBinder;
import net.hasor.web.binder.MappingDef;

/**
 * Registers business APIs, management, UI and API specifications in an existing Hasor application.
 * Reads routing prefixes from Hasor Settings, including hconfig.xml. Entry switches are owned by this integration.
 */
public final class DatawayModule implements Module {
    private static final String        CONFIG_PREFIX = "dataway.";
    private final        DatawayConfig config;
    private final        Dataway       dataway;

    public DatawayModule() {
        this.config = null;
        this.dataway = null;
    }

    public DatawayModule(DatawayConfig config) {
        this.config = Objects.requireNonNull(config);
        this.dataway = null;
    }

    public DatawayModule(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
        this.config = null;
    }

    @Override
    public void loadModule(ApiBinder binder) {
        Settings settings = binder.getSettings();
        boolean apiEnabled = settings.getBoolean(CONFIG_PREFIX + "api-enabled", false);
        boolean adminEnabled = settings.getBoolean(CONFIG_PREFIX + "admin-enabled", false);
        boolean docsEnabled = settings.getBoolean(CONFIG_PREFIX + "docs-enabled", false);
        Dataway dataway = this.dataway;
        if (!apiEnabled && !adminEnabled && !docsEnabled && dataway == null) {
            return;
        }

        Supplier<Dataway> provider;
        if (dataway == null) {
            Supplier<DatawayConfig> configuration = this.config == null ? binder.getProvider(DatawayConfig.class) : () -> this.config;

            Supplier<ApiDataAccessLayer> metadata = binder.getProvider("", ApiDataAccessLayer.class);
            var factory = Provider.of((Callable<Dataway>) () -> {
                DatawayConfig config = configuration.get();
                if (config.getDataAccessLayer() == null) {
                    config.dataAccessLayer(metadata.get());
                }
                return config.createDataway();
            }).asSingle();

            var info = binder.bindType(Dataway.class).toProvider(factory).asEagerSingleton().toInfo();
            binder.lazyLoad(context -> context.getInstance(info));
            provider = binder.getProvider(info);
        } else {
            binder.bindType(Dataway.class).toInstance(dataway);
            provider = () -> this.dataway;
        }

        WebApiBinder web = binder.tryCast(WebApiBinder.class);
        if (web == null) {
            return; // Java-only Hasor hosts can use the assembled core directly.
        }

        // API
        if (apiEnabled) {
            String prefix = settings.getString(CONFIG_PREFIX + "api-prefix", "/api");
            this.register(web, prefix, provider, Dataway::getApiHandler);
        }

        // API specifications
        if (docsEnabled) {
            String prefix = settings.getString(CONFIG_PREFIX + "docs-prefix", "/docs");
            this.register(web, prefix, provider, Dataway::getDocumentHandler);
        }

        // Admin API and UI
        if (adminEnabled) {
            String prefix = settings.getString(CONFIG_PREFIX + "admin-prefix", "/admin/api");
            this.register(web, prefix, provider, Dataway::getAdminHandler);

            // Register the UI wildcard after the API and document routes.
            String uiPrefix = settings.getString(CONFIG_PREFIX + "admin-ui", "/admin");
            this.register(web, uiPrefix, provider, Dataway::getAdminUiHandler);
        }
    }

    private void register(WebApiBinder web, String prefix, Supplier<Dataway> provider, Function<Dataway, WebHandler> entry) {
        if (this.dataway != null) {
            WebHandler handler = entry.apply(this.dataway);
            String[] paths = handler.paths().stream().map(path -> prefix + path).toArray(String[]::new);
            web.mappingTo(paths).with(new DatawayController(prefix, handler));
            return;
        }

        var controller = web.bindType(DatawayController.class).uniqueName().toProvider(() -> {
            return new DatawayController(prefix, entry.apply(provider.get()));
        }).toInfo();
        web.bindType(MappingDef.class).uniqueName().toProvider(() -> {
            List<String> paths = entry.apply(provider.get()).paths().stream().map(path -> prefix + path).toList();
            return new DatawayMapping(controller, paths);
        });
    }
}
