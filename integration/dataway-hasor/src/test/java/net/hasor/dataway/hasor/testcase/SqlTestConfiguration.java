/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.AppContext;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.hasor.example.config.DatawayConfiguration;
import net.hasor.dataway.service.DatawayConfig;

/** Reuses the example's SQL configuration for isolated HTTP scenarios. */
@Configuration
public class SqlTestConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections) {
        return new DatawayConfiguration().datawayConfig(identityProvider, connections);
    }

    @Bean
    public ConnectionProvider connectionProvider(AppContext appContext) {
        return new DatawayConfiguration().connectionProvider(appContext);
    }
}
