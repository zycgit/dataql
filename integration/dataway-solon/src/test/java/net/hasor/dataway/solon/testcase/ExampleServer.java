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
import java.util.UUID;
import javax.sql.DataSource;
import net.hasor.dataway.solon.example.ExampleApplication;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.core.AppContext;

/** Boots the unchanged standalone application with isolated databases and a free port. */
public class ExampleServer implements AutoCloseable {
    private final SimpleSolonApp application;
    private final int            port;

    public ExampleServer() throws Throwable {
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            this.port = socket.getLocalPort();
        }
        String database = "jdbc:h2:mem:" + UUID.randomUUID();
        this.application = new SimpleSolonApp(ExampleApplication.class, "--cfg=example/app.properties", "--server.port=" + this.port, "--example.database.main.url=" + database + "-main", "--example.database.ds1.url=" + database + "-ds1", "--example.database.ds2.url=" + database + "-ds2").globalize(true);
        try {
            this.application.start(app -> app.enableHttp(true));
        } catch (Throwable failure) {
            this.application.stop();
            throw failure;
        }
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.port;
    }

    public AppContext context() {
        return this.application.context();
    }

    public DataSource source(String name) {
        return this.context().getBean(name);
    }

    @Override
    public void close() {
        this.application.stop();
    }
}
