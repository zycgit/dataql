/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example;
import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;
import net.hasor.config.Configuration;

/** Starts the example using its XML settings and annotation configuration. */
@Configuration
public class ExampleApplication {
    public static void main(String[] args) throws Exception {
        Boot boot = new Boot()                      //
                .hconfigFile("example/hconfig.xml") //
                .sources(ExampleApplication.class)  //
                .arguments(args);

        try (BootApplication application = boot.start()) {
            application.join();
        }
    }
}
