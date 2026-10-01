/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.example;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import net.hasor.dataway.spring.nacos.testcase.NacosTestServer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/** Run from IDEA to start both the example and its local Nacos server. */
public class TestExampleApplication {
    public static void main(String[] args) throws Exception {
        NacosTestServer nacos = new NacosTestServer(Path.of("target"));
        try {
            var arguments = new ArrayList<>(Arrays.asList(args));
            arguments.add("--example.nacos.server-addr=" + nacos.serverAddress());
            arguments.add("--spring.config.location=classpath:application.yml");
            SpringApplication application = new SpringApplication(ExampleApplication.class);
            application.addListeners((ApplicationListener<ContextClosedEvent>) event -> nacos.close());
            application.run(arguments.toArray(String[]::new));
        } catch (Exception error) {
            nacos.close();
            throw error;
        }
    }
}
