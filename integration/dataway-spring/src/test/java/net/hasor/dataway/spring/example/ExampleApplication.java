/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Run this application to explore Dataway with a browser at http://127.0.0.1:8080/. */
@SpringBootApplication
public class ExampleApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ExampleApplication.class);
        application.setDefaultProperties(Map.of("spring.config.location", "classpath:example/application.yml"));
        application.run(args);
    }
}
