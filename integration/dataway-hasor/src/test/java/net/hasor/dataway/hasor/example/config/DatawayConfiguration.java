/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.config;
import javax.sql.DataSource;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.ApiBinder;
import net.hasor.core.AppContext;
import net.hasor.core.Module;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.hasor.DatawayModule;
import net.hasor.dataway.hasor.HasorTransactionProvider;
import net.hasor.dataway.hasor.example.config.auth.LoginInterceptor;
import net.hasor.dataway.hasor.example.service.ExampleApiService;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;

/** Connects Dataway to the application's identity, metadata and SQL services. */
@Configuration
public class DatawayConfiguration implements Module {

    @Override
    public void loadModule(ApiBinder binder) throws Throwable {
        binder.installModule(new DatawayModule());
    }

    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections) {
        return new DatawayConfig()                  //
                .identityProvider(identityProvider) //
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }

    @Bean
    public ApiDataAccessLayer metadata(DataSource mainSource) {
        return new JdbcDataAccessLayer(mainSource);
    }

    /** Uses the same named data sources and transactions as the host application. */
    @Bean
    public ConnectionProvider connectionProvider(AppContext appContext) {
        return new HasorTransactionProvider(appContext);
    }

    //
    // for example
    //

    @Bean(initMethod = "initialize")
    public ExampleApiService exampleApis(Dataway dataway) {
        return new ExampleApiService(dataway.getAdminService());
    }
}
