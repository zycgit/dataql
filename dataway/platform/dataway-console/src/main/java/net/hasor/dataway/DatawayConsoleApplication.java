/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway;

import javax.sql.DataSource;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.loader.providers.PrefixResourceLoader;
import net.hasor.cobble.setting.Settings;
import net.hasor.boot.web.WebServerConfig;
import net.hasor.boot.web.WebServers;
import net.hasor.config.ApplicationBoot;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.config.web.WebMvcConfigurer;
import net.hasor.config.web.render.JsonRenderConfigurer;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import net.hasor.web.WebApiBinder;
import net.hasor.web.render.json.JsonRenderEngine;

/**
 * Dataway Console boot application.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2026-07-15
 */
@Configuration
public class DatawayConsoleApplication implements WebMvcConfigurer {
    public static void main(String[] args) throws Exception {
        String jdbcUrl = setting("dataway.datasource.jdbc-url", "DATAWAY_JDBC_URL", //
                "jdbc:mysql://127.0.0.1:3306/dataway?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai");
        String username = setting("dataway.datasource.username", "DATAWAY_JDBC_USERNAME", "root");
        String password = setting("dataway.datasource.password", "DATAWAY_JDBC_PASSWORD", "");
        String connectionTimeout = setting("dataway.datasource.connection-timeout", "DATAWAY_CONNECTION_TIMEOUT", "10000");
        int port = Integer.parseInt(setting("hasor.http.port", "DATAWAY_HTTP_PORT", "8080"));

        WebServerConfig serverConfig = new WebServerConfig().port(port).arguments(args).appContextFactory(servletContext -> {
            return ApplicationBoot.create(servletContext, DatawayConsoleApplication.class)//
                    .addSettings(Settings.DefaultNameSpace, "dataway.datasource.jdbc-url", jdbcUrl)//
                    .addSettings(Settings.DefaultNameSpace, "dataway.datasource.username", username)//
                    .addSettings(Settings.DefaultNameSpace, "dataway.datasource.password", password)//
                    .addSettings(Settings.DefaultNameSpace, "dataway.datasource.connection-timeout", connectionTimeout)//
                    .bindArguments(args)//
                    .build();
        });
        WebServers.run(serverConfig).join();
    }

    @Bean
    public HostConfiguration hostConfiguration(DataSource dataSource) {
        HostConfiguration configuration = new HostConfiguration();
        configuration.addAttachment(ConnectionProvider.class, (sourceName, hints) -> dataSource.getConnection());
        return configuration;
    }

    @Bean
    public QueryManager queryManager(HostConfiguration configuration) {
        return new QueryManager(configuration.getHostContext());
    }

    @Override
    public void loadModule(WebApiBinder webBinder) {
        WebMvcConfigurer.super.loadModule(webBinder);
        webBinder.setEncodingCharacter("UTF-8", "UTF-8");
    }

    @Override
    public void addResourceHandlers(WebApiBinder binder) {
        var resources = new PrefixResourceLoader(binder.getResourceLoader(), "META-INF/hasor-framework/dataway-ui/");
        binder.addResource("/interface-ui/**", resources).welcomeFile("index.html");
    }

    @Override
    public void configureJson(JsonRenderConfigurer configurer) {
        configurer.renderEngine(JsonRenderEngine.class);
    }

    private static String setting(String systemProperty, String environment, String defaultValue) {
        String value = StringUtils.trimToNull(System.getProperty(systemProperty));
        if (value == null) {
            value = StringUtils.trimToNull(System.getenv(environment));
        }
        return value == null ? defaultValue : value;
    }
}
