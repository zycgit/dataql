/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.example.config;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.setting.Settings;
import net.hasor.config.Bean;
import net.hasor.config.Configuration;
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;
import net.hasor.dbvisitor.hasor.session.SessionConfigurer;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;

/** Supplies the default application data source and two named sources with the same dbVisitor services. */
@Configuration
public class DatabaseConfiguration implements Module {

    @Override
    public void loadModule(ApiBinder binder) throws Throwable {
        /*
         * Registers dbVisitor services for the DataSource beans declared below.
         * The Bean methods create and initialize connection pools; SessionConfigurer
         * reuses those pools to provide JdbcTemplate, TransactionTemplate, Session
         * and the other database services for each source.
         * The no-argument constructor selects the unnamed default DataSource.
         * The names "ds1" and "ds2" match the corresponding Bean names and are
         * also used when looking up their database and transaction services.
         */
        binder.installModule(new SessionConfigurer());
        binder.installModule(new SessionConfigurer("ds1"));
        binder.installModule(new SessionConfigurer("ds2"));
    }

    @Bean(destroyMethod = "dispose")
    public DataSource mainSource(Settings settings) throws Exception {
        return this.createSource(settings, "main", "/META-INF/dataway/schema/h2.sql");
    }

    @Bean(value = "ds1", destroyMethod = "dispose")
    public DataSource ds1(Settings settings) throws Exception {
        return this.createSource(settings, "ds1", "/example/database/people.sql");
    }

    @Bean(value = "ds2", destroyMethod = "dispose")
    public DataSource ds2(Settings settings) throws Exception {
        return this.createSource(settings, "ds2", "/example/database/orders.sql");
    }

    private DataSource createSource(Settings settings, String name, String schema) throws Exception {
        String prefix = "example.database." + name + ".";
        String jdbcUrl = settings.getString(prefix + "url", "jdbc:h2:mem:dataway-example-" + name);
        String username = settings.getString(prefix + "username", "sa");
        String password = settings.getString(prefix + "password", "");

        JdbcConnectionPool source = JdbcConnectionPool.create(jdbcUrl, username, password);
        try (var connection = source.getConnection();//
             var input = ResourcesUtils.getResourceAsStream(schema);//
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {

            RunScript.execute(connection, reader);
            return source;
        } catch (Exception | Error failure) {
            source.dispose();
            throw failure;
        }
    }
}