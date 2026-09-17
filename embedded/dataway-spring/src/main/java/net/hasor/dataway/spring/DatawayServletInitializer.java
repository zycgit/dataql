/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;

import java.util.Objects;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.web.WebHandler;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.Ordered;

/** Mounts the core's entries as independent servlet filters. */
public final class DatawayServletInitializer implements ServletContextInitializer, Ordered {
    private final Dataway dataway;

    public DatawayServletInitializer(Dataway dataway) {
        this.dataway = Objects.requireNonNull(dataway);
    }

    @Override
    public int getOrder() {
        // Keep Dataway after the host's authentication filters.
        return 100;
    }

    @Override
    public void onStartup(ServletContext context) throws ServletException {
        for (var entry : this.dataway.getHandlers().entrySet()) {
            WebHandler handler = entry.getValue();
            var registration = new FilterRegistrationBean<>(new DatawayFilter(handler));
            registration.setName(entry.getKey().getRegistrationName());
            registration.addUrlPatterns(handler.pathPrefix(), handler.pathPrefix() + "/*");
            registration.onStartup(context);
        }
    }
}