/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example.config;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import net.hasor.dataway.spring.example.service.UserService;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** The primary database stores metadata; ds1 and ds2 contain independent business data. */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class DatabaseConfiguration {
    @Bean(destroyMethod = "dispose")
    @Primary
    public DataSource dataSource(Environment settings) throws Exception {
        return this.createSource(settings, "main", "/META-INF/dataway/schema/h2.sql", "/database/users.sql");
    }

    @Bean(name = "ds1", destroyMethod = "dispose")
    public DataSource ds1(Environment settings) throws Exception {
        return this.createSource(settings, "ds1", "/database/people.sql");
    }

    @Bean(name = "ds2", destroyMethod = "dispose")
    public DataSource ds2(Environment settings) throws Exception {
        return this.createSource(settings, "ds2", "/database/orders.sql");
    }

    private DataSource createSource(Environment settings, String name, String... schemas) throws Exception {
        String prefix = "example.database." + name;
        String url = settings.getRequiredProperty(prefix + ".url");
        String username = settings.getProperty(prefix + ".username", "sa");
        String password = settings.getProperty(prefix + ".password", "");
        JdbcConnectionPool source = JdbcConnectionPool.create(url, username, password);
        try (var connection = source.getConnection()) {
            for (String schema : schemas) {
                try (var input = DatabaseConfiguration.class.getResourceAsStream(schema)) {
                    if (input == null) {
                        throw new IllegalStateException("Schema not found: " + schema);
                    }
                    RunScript.execute(connection, new InputStreamReader(input, StandardCharsets.UTF_8));
                }
            }
            return source;
        } catch (Exception failure) {
            source.dispose();
            throw failure;
        }
    }

    // Each source has the same Spring JDBC and transaction services.
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource source) {
        return new JdbcTemplate(source);
    }

    @Bean
    public JdbcTemplate ds1JdbcTemplate(@Qualifier("ds1") DataSource source) {
        return new JdbcTemplate(source);
    }

    @Bean
    public JdbcTemplate ds2JdbcTemplate(@Qualifier("ds2") DataSource source) {
        return new JdbcTemplate(source);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public PlatformTransactionManager ds1TransactionManager(@Qualifier("ds1") DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public PlatformTransactionManager ds2TransactionManager(@Qualifier("ds2") DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public UserService users(DataSource source) {
        return new UserService(source);
    }
}
