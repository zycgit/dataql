/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.testcase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/** A real Spring Boot servlet application, including auto-configuration and Tomcat. */
public final class TestApplication implements AutoCloseable {
    private final ServletWebServerApplicationContext context;

    public TestApplication(DatawayConfig config, ApiDataAccessLayer storage, Properties settings) {
        this(config, storage, settings, context -> {
        });
    }

    public TestApplication(DatawayConfig config, ApiDataAccessLayer storage, Properties settings, Consumer<GenericApplicationContext> initializer) {
        SpringApplication application = new SpringApplication(TestBootConfiguration.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setRegisterShutdownHook(false);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("server.servlet.context-path", "/host");
        properties.put("spring.main.log-startup-info", "false");
        properties.put("logging.level.root", "WARN");
        settings.forEach((key, value) -> properties.put(key.toString(), value));
        application.setDefaultProperties(properties);
        application.addInitializers(context -> {
            var beans = (GenericApplicationContext) context;
            if (config != null) {
                beans.registerBean(DatawayConfig.class, () -> config);
            }
            if (storage != null) {
                beans.registerBean("metadata", ApiDataAccessLayer.class, () -> storage);
            }
            initializer.accept(beans);
        });
        this.context = (ServletWebServerApplicationContext) application.run();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + this.context.getWebServer().getPort() + "/host";
    }

    public ServletWebServerApplicationContext context() {
        return this.context;
    }

    public Dataway dataway() {
        return this.context.getBean(Dataway.class);
    }

    @Override
    public void close() {
        this.context.close();
    }
}
