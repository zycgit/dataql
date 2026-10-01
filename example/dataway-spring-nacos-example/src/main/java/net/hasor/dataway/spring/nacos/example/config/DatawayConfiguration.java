/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.example.config;
import java.nio.file.Path;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.dataway.authorization.IdentityProvider;
import net.hasor.dataway.authorization.RequestIdentityProvider;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.spring.SpringTransactionProvider;
import net.hasor.dataway.spring.nacos.example.config.auth.LoginInterceptor;
import net.hasor.dataway.spring.nacos.example.service.ExampleApiService;
import net.hasor.dataway.spring.nacos.example.service.UploadFunctions;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DatawayConfiguration {
    @Bean
    public DatawayConfig datawayConfig(IdentityProvider identityProvider, ConnectionProvider connections, Environment settings) {
        return new DatawayConfig()//
                .identityProvider(identityProvider)//
                .documentServer(settings.getProperty("dataway.api-prefix", "/api"))//
                .uploadTempDirectory(Path.of(settings.getProperty("example.upload.directory", "./target/uploads")))//
                .uploadMemoryThreshold(Integer.parseInt(settings.getProperty("example.upload.memory-threshold", "65536")))//
                .importSource("example.Upload", UploadFunctions::new)//
                .attachment(ConnectionProvider.class, connections);
    }

    @Bean
    public IdentityProvider identityProvider() {
        return new RequestIdentityProvider(LoginInterceptor.IDENTITY_ATTRIBUTE);
    }

    /** Named SQL fragments use ds1/ds2; unnamed fragments use the primary application source. */
    @Bean
    public ConnectionProvider connectionProvider(ApplicationContext context) {
        return new SpringTransactionProvider(context);
    }

    @Bean(initMethod = "initialize")
    public ExampleApiService exampleApis(Dataway dataway) {
        return new ExampleApiService(dataway.getAdminService());
    }
}
