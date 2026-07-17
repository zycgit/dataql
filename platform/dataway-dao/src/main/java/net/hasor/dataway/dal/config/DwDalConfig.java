/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private DataSource createDriverDataSource(String jdbcUrl, String username) {
        DefaultDataSource dataSource = new DefaultDataSource();
        dataSource.setDriverClassName(MYSQL_DRIVER);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(StringUtils.defaultString(this.password));
        return dataSource;
    }
}
