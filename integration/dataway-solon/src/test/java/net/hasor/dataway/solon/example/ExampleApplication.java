/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example;
import org.noear.solon.Solon;
import org.noear.solon.core.util.MultiMap;

/** Run this application to explore Dataway with a browser at http://127.0.0.1:8080/. */
public class ExampleApplication {
    public static void main(String[] args) {
        MultiMap<String> arguments = MultiMap.from(args);
        arguments.putIfAbsent("cfg", "example/app.properties");
        Solon.start(ExampleApplication.class, arguments);
    }
}
