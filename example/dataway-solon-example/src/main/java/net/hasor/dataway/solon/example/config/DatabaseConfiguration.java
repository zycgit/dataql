/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import net.hasor.dataway.solon.example.service.UserService;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.Props;

/** The primary database stores metadata; ds1 and ds2 contain independent business data. */
@Configuration
public class DatabaseConfiguration {
    @Bean(destroyMethod = "dispose")
    public DataSource dataSource(@Inject("${example.database}") Props settings) throws Exception {
        return this.createSource(settings, "main", "/META-INF/dataway/schema/h2.sql", "/database/users.sql");
    }

    @Bean(name = "ds1", typed = false, destroyMethod = "dispose")
    public DataSource ds1(@Inject("${example.database}") Props settings) throws Exception {
        return this.createSource(settings, "ds1", "/database/people.sql");
    }

    @Bean(name = "ds2", typed = false, destroyMethod = "dispose")
    public DataSource ds2(@Inject("${example.database}") Props settings) throws Exception {
        return this.createSource(settings, "ds2", "/database/orders.sql");
    }

    private DataSource createSource(Props settings, String name, String... schemas) throws Exception {
        String prefix = name;
        String url = settings.get(prefix + ".url");
        String username = settings.get(prefix + ".username", "sa");
        String password = settings.get(prefix + ".password", "");

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
    @Bean
    public UserService users(DataSource source) {
        return new UserService(source);
    }
}
