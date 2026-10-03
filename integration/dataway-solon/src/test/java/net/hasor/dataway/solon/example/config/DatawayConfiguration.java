/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import javax.sql.DataSource;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.solon.DatawayPlugin;
import net.hasor.dataway.solon.SolonTransactionProvider;
import net.hasor.dataway.solon.example.config.auth.LoginInterceptor;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

@Configuration
public class DatawayConfiguration {
    @Inject
    private AppContext    context;
    @Inject
    private DatawayConfig config;

    @Init
    public void initialize() {
        // The plugin builds Dataway after ordinary bean initialization has completed.
        new DatawayPlugin(this.config).start(this.context);
    }

    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections) {
        return new DatawayConfig()//
                .identityProvider(identityProvider)//
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }

    @Bean
    public ApiDataAccessLayer metadata(DataSource source) {
        return new JdbcDataAccessLayer(source);
    }

    /** Named SQL fragments use ds1/ds2; unnamed fragments use the default application source. */
    @Bean
    public ConnectionProvider connectionProvider(AppContext context) {
        return new SolonTransactionProvider(context);
    }
}
