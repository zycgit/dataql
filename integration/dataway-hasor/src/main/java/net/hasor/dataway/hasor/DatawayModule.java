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
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.model.WebHandler;
import net.hasor.web.WebApiBinder;
import net.hasor.web.binder.MappingDef;

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

        Supplier<Dataway> provider;
        if (dataway == null) {
            var factory = Provider.of((Callable<Dataway>) this.builder::build).asSingle();
            var binding = binder.bindType(Dataway.class).toProvider(factory);
            if (this.builder.requiresDefaultDataAccessLayer()) {
                String name = settings.getString(CONFIG_PREFIX + "metadata.bean", "").trim();
                this.builder.defaultDataAccessLayer(binder.getProvider(name, ApiDataAccessLayer.class));
                if (name.isEmpty()) {
                    binding.dependsOn(ApiDataAccessLayer.class);
                } else {
                    binding.dependsOn(name, ApiDataAccessLayer.class);
                }
            }

            var info = binding.asEagerSingleton().toInfo();
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

        // Admin API
        if (adminEnabled) {
            String prefix = settings.getString(CONFIG_PREFIX + "admin-prefix", "/dataway/api");
            this.register(web, prefix, provider, Dataway::getAdminHandler);

            // Admin UI
            String uiPrefix = settings.getString(CONFIG_PREFIX + "admin-ui", "/dataway");
            this.register(web, uiPrefix, provider, Dataway::getUiHandler);
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
