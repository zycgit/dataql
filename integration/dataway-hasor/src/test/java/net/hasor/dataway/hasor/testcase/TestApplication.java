/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;
import net.hasor.boot.web.WebServer;
import net.hasor.core.AppContext;
import net.hasor.core.Module;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.hasor.example.config.WebConfiguration;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

/** Starts isolated Boot instances with per-test configuration and storage overrides. */
public final class TestApplication implements AutoCloseable {
    private final BootApplication application;

    public TestApplication(DatawayConfig config, ApiDataAccessLayer storage, Properties settings) throws Exception {
        this(config, null, storage, settings);
    }

    public TestApplication(Module module, ApiDataAccessLayer storage, Properties settings, Class<?>... configurations) throws Exception {
        this(TestSettings.configuration(), module, storage, settings, configurations);
    }

    public TestApplication(DatawayConfig config, ApiDataAccessLayer storage, Properties settings, Class<?>... configurations) throws Exception {
        this(config, null, storage, settings, configurations);
    }

    private TestApplication(DatawayConfig config, Module module, ApiDataAccessLayer storage, Properties settings, Class<?>... configurations) throws Exception {
        Boot boot = new Boot().property("hasor.loadPackages", "net.hasor.dataway.hasor.example.web").property("hasor.boot.web.connectors.http.host", "127.0.0.1").property("hasor.boot.web.connectors.http.port", 0).property("hasor.boot.web.server.contextPath", "/host");
        List<Class<?>> sources = new ArrayList<>();
        sources.add(WebConfiguration.class);
        sources.add(TestConfiguration.class);
        sources.addAll(List.of(configurations));
        settings.forEach((key, value) -> boot.property(key.toString(), value));
        TestBootstrap.CURRENT.set(new TestBootstrap(config == null ? TestSettings.configuration() : config, storage, module));
        try {
            this.application = boot.sources(sources.toArray(Class<?>[]::new)).start();
        } finally {
            TestBootstrap.CURRENT.remove();
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.context().getInstance(WebServer.class).getPort() + "/host";
    }

    public AppContext context() {
        return this.application.getAppContext();
    }

    public Dataway dataway() {
        return this.context().getInstance(Dataway.class);
    }

    @Override
    public void close() throws Exception {
        this.application.close();
    }
}
