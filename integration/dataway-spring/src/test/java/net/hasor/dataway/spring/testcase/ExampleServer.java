/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import net.hasor.dataway.spring.example.ExampleApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

/** Boots the unchanged standalone application with isolated databases and an ephemeral port. */
public class ExampleServer implements AutoCloseable {
    private final ServletWebServerApplicationContext context;

    public ExampleServer() {
        String database = "jdbc:h2:mem:" + UUID.randomUUID();
        SpringApplication app = new SpringApplication(ExampleApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setRegisterShutdownHook(false);
        app.setDefaultProperties(Map.of("spring.config.location", "classpath:example/application.yml"));
        this.context = (ServletWebServerApplicationContext) app.run("--server.port=0", "--example.database.main.url=" + database + "-main", "--example.database.ds1.url=" + database + "-ds1", "--example.database.ds2.url=" + database + "-ds2", "--logging.level.root=WARN");
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.context.getWebServer().getPort();
    }

    public ServletWebServerApplicationContext context() {
        return this.context;
    }

    public DataSource source(String name) {
        return this.context.getBean(name, DataSource.class);
    }

    @Override
    public void close() {
        this.context.close();
    }
}
