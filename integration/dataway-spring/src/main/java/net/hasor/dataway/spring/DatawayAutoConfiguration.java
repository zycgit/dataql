/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import javax.sql.DataSource;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.DatawayBuilder;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;
import net.hasor.dataway.spi.DatawayConfigurer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Assembles the shared core on demand and registers MVC controller entries.
 * Entry switches default to false; routing configuration stays in this integration.
 */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class DatawayAutoConfiguration {
    private static final String CONFIG_PREFIX = "dataway";

    @Bean
    @Lazy
    @ConditionalOnMissingBean(Dataway.class)
    public Dataway dataway(ObjectProvider<DataSource> sources, ListableBeanFactory beans, ObjectProvider<DatawayConfigurer> configurers) {
        DatawayBuilder builder = Dataway.builder().dataSource(sources::getIfUnique).dataSources(() -> {
            return beans.getBeansOfType(DataSource.class);
        });

        builder.defaultDatabaseExecutor(source -> {
            JdbcExecutor executor = beans.getBeanProvider(JdbcExecutor.class).getIfAvailable();
            if (executor != null) {
                return executor;
            }
            PlatformTransactionManager manager = beans.getBeanProvider(PlatformTransactionManager.class).getIfUnique();
            if (manager == null) {
                var managers = beans.getBeansOfType(PlatformTransactionManager.class);
                if (!managers.isEmpty()) {
                    throw new IllegalStateException("Select the Dataway transaction manager through JdbcExecutor");
                }
                manager = new JdbcTransactionManager(source);
            }
            return new SpringJdbcExecutor(source, manager);
        });

        configurers.orderedStream().forEach(builder::configure);
        return builder.build();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public DatawayMvcRegistrar datawayMvcRegistrar(ObjectProvider<Dataway> cores, Environment environment, @Qualifier("requestMappingHandlerMapping") ObjectProvider<RequestMappingHandlerMapping> mappings) {
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
