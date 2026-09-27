/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayBuilder;
import net.hasor.dataway.service.DatawayConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Assembles the shared core on demand and registers MVC controller entries.
 * Entry switches default to false; routing configuration stays in this integration.
 */
@AutoConfiguration
public class DatawayAutoConfiguration {
    private static final String CONFIG_PREFIX = "dataway";

    @Bean
    @Lazy
    @ConditionalOnMissingBean(Dataway.class)
    public Dataway dataway(ConfigurableListableBeanFactory beans, //
            ObjectProvider<DatawayConfigurer> configurers, Environment environment) {
        DatawayBuilder builder = Dataway.builder();
        builder.defaultDataAccessLayer(() -> this.getDataAccessLayer(beans, environment));
        configurers.orderedStream().forEach(builder::configure);
        return builder.build();
    }

    private ApiDataAccessLayer getDataAccessLayer(ConfigurableListableBeanFactory beans, Environment environment) {
        String name = Binder.get(environment).bind("dataway.metadata.bean", String.class).orElse("").trim();
        return name.isEmpty() ? beans.getBean(ApiDataAccessLayer.class) : beans.getBean(name, ApiDataAccessLayer.class);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public DatawayMvcRegistrar datawayMvcRegistrar(ObjectProvider<Dataway> cores, Environment environment, ObjectProvider<RequestMappingHandlerMapping> mappings) {
        Binder binder = Binder.get(environment);
        boolean apiEnabled = binder.bind(CONFIG_PREFIX + ".api-enabled", Boolean.class).orElse(false);
        boolean adminEnabled = binder.bind(CONFIG_PREFIX + ".admin-enabled", Boolean.class).orElse(false);
        Dataway dataway = apiEnabled || adminEnabled ? cores.getObject() : null;
        String apiPrefix = binder.bind(CONFIG_PREFIX + ".api-prefix", String.class).orElse("/api");
        String adminPrefix = binder.bind(CONFIG_PREFIX + ".admin-prefix", String.class).orElse("/dataway/api");
        String uiPrefix = binder.bind(CONFIG_PREFIX + ".admin-ui", String.class).orElse("/dataway");
        return new DatawayMvcRegistrar(dataway, apiEnabled, adminEnabled, apiPrefix, adminPrefix, uiPrefix, mappings);
    }
}
