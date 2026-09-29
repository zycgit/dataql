/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Properties;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.solon.DatawayPlugin;
import net.hasor.dataway.solon.example.config.WebConfiguration;
import net.hasor.dataway.solon.example.config.auth.LoginInterceptor;
import net.hasor.dataway.solon.example.web.LoginController;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.core.Plugin;

/** Solon loads the HTTP server plugin and exposes actual listening sockets. */
public final class TestApplication implements AutoCloseable {
    private final SimpleSolonApp app;
    private final int            port;

    public TestApplication(DatawayConfig config, ApiDataAccessLayer storage, Properties settings) throws Throwable {
        this(config == null ? new DatawayPlugin() : new DatawayPlugin(config), storage, settings);
    }

    public TestApplication(Plugin plugin, ApiDataAccessLayer storage, Properties settings) throws Throwable {
        // Smart HTTP's Signal reports the configured port, so choose a free port before startup.
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            this.port = socket.getLocalPort();
        }
        this.app = new SimpleSolonApp(TestApplication.class, "--server.port=" + this.port, "--server.host=127.0.0.1", "--server.http.coreThreads=2").globalize(true);
        try {
            this.app.start(app -> {
                app.enableHttp(true).enableScanning(false);
                app.cfg().putAll(settings);
                if (storage != null) {
                    app.context().wrapAndPut(ApiDataAccessLayer.class, storage);
                }
                app.context().wrapAndPut(IdentityProvider.class, new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE));
                app.context().beanMake(WebConfiguration.class);
                app.context().beanScan(LoginController.class);
                app.pluginAdd(0, plugin);
            });
        } catch (Throwable error) {
            this.app.stop();
            throw error;
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.port;
    }

    public SimpleSolonApp context() {
        return this.app;
    }

    public Dataway dataway() {
        return this.app.context().getBean(Dataway.class);
    }

    @Override
    public void close() {
        this.app.stop();
    }
}
