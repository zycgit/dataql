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
import net.hasor.dataway.DatawayConfigurer;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/** Supplies host configuration and resources to the core, then mounts its HTTP entries. */
@AutoConfiguration(afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class DatawayAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(Dataway.class)
    public Dataway dataway(Environment env, ObjectProvider<DataSource> sources, ListableBeanFactory beans, ObjectProvider<DatawayConfigurer> configurers) {
        DatawayBuilder builder = Dataway.builder().configuration(env::getProperty).dataSource(sources::getIfUnique).dataSources(() -> {
            return beans.getBeansOfType(DataSource.class);
        });

        configurers.orderedStream().forEach(builder::configure);
        return builder.build();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public DatawayServletInitializer datawayServletInitializer(Dataway dataway) {
        return new DatawayServletInitializer(dataway);
    }
}
