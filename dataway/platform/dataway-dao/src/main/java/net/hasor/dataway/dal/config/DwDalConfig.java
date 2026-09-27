/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.config;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.hasor.cobble.StringUtils;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.ApiBinder;
import net.hasor.core.InjectSettings;
import net.hasor.core.Module;
import net.hasor.dataway.dal.access.ApiDal;
import net.hasor.dataway.dal.access.AuthDal;
import net.hasor.dataway.dal.access.DataSourceDal;
import net.hasor.dataway.dal.access.impl.ApiDalImpl;
import net.hasor.dataway.dal.access.impl.AuthDalImpl;
import net.hasor.dataway.dal.access.impl.DataSourceDalImpl;
import net.hasor.dataway.dal.mapper.api.DwInterfaceHistoryMapper;
import net.hasor.dataway.dal.mapper.api.DwInterfaceInfoMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthRoleMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthUserMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsConfigMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsMapper;
import net.hasor.dbvisitor.hasor.autoconfig.DefaultDataSource;
import net.hasor.dbvisitor.hasor.mapper.MapperScannerConfigurer;
import net.hasor.dbvisitor.hasor.session.SessionConfigurer;
import net.hasor.dbvisitor.mapper.SimpleMapper;

/** Configures the Dataway data source and dbVisitor mapper integration. */
@Configuration
public class DwDalConfig implements Module {
    private static final String MYSQL_DRIVER      = "com.mysql.cj.jdbc.Driver";
    private static final int    MINIMUM_IDLE      = 1;
    private static final int    MAXIMUM_POOL_SIZE = 20;

    @InjectSettings("dataway.datasource.jdbc-url")
    private String jdbcUrl;
    @InjectSettings("dataway.datasource.username")
    private String username;
    @InjectSettings("dataway.datasource.password")
    private String password;
    @InjectSettings("dataway.datasource.connection-timeout")
    private long   connectionTimeout;

    @Override
    public void loadModule(ApiBinder apiBinder) throws Throwable {
        apiBinder.installModule(new SessionConfigurer());

        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        scanner.setBasePackage("net.hasor.dataway.dal.mapper");
        scanner.setAnnotationClass(SimpleMapper.class);
        apiBinder.installModule(scanner);
    }

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        String actualJdbcUrl = StringUtils.trimToNull(this.jdbcUrl);
        String actualUsername = StringUtils.trimToNull(this.username);
        if (actualJdbcUrl == null || actualUsername == null) {
            throw new IllegalArgumentException("jdbcUrl/username is blank.");
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(this.createDriverDataSource(actualJdbcUrl, actualUsername));
        hikariConfig.setConnectionTimeout(this.connectionTimeout);
        hikariConfig.setMinimumIdle(MINIMUM_IDLE);
        hikariConfig.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        try {
            new DwDbMigration(dataSource).migrate();
            return dataSource;
        } catch (RuntimeException | Error e) {
            dataSource.close();
            throw e;
        }
    }

    @Bean
    public ApiDal apiDal(DwInterfaceInfoMapper infoMapper, DwInterfaceHistoryMapper historyMapper) {
        return new ApiDalImpl(infoMapper, historyMapper);
    }

    @Bean
    public AuthDal authDal(DwAuthUserMapper userMapper, DwAuthRoleMapper roleMapper) {
        return new AuthDalImpl(userMapper, roleMapper);
    }

    @Bean
    public DataSourceDal dataSourceDal(DwDsMapper dataSourceMapper, DwDsConfigMapper configMapper) {
        return new DataSourceDalImpl(dataSourceMapper, configMapper);
    }

    private DataSource createDriverDataSource(String jdbcUrl, String username) {
        DefaultDataSource dataSource = new DefaultDataSource();
        dataSource.setDriverClassName(MYSQL_DRIVER);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(StringUtils.defaultString(this.password));
        return dataSource;
    }
}
