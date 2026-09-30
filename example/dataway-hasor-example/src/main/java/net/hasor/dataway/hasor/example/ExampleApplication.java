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
import net.hasor.dataway.hasor.example.config.DatabaseConfiguration;
import net.hasor.dataway.hasor.example.config.DatawayConfiguration;
import net.hasor.dataway.hasor.example.config.MetadataConfiguration;
import net.hasor.dataway.hasor.example.config.WebConfiguration;

/** Starts the example using its XML settings and annotation configuration. */
@Configuration
public class ExampleApplication {
    public static void main(String[] args) throws Exception {
        Boot boot = new Boot()                      //
                .hconfigFile("hconfig.xml") //
                .sources(DatabaseConfiguration.class, MetadataConfiguration.class,
                        DatawayConfiguration.class, WebConfiguration.class) //
                .arguments(args);

        // Boot exposes application arguments separately; map this demo's --key=value overrides to settings.
        for (String argument : args) {
            int separator = argument.indexOf('=');
            if (argument.startsWith("--") && separator > 2) {
                boot.property(argument.substring(2, separator), argument.substring(separator + 1));
            }
        }

        try (BootApplication application = boot.start()) {
            application.join();
        }
    }
}
