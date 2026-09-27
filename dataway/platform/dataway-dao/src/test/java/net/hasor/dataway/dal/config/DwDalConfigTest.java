/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.config;

import com.zaxxer.hikari.HikariDataSource;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.AppContext;
import net.hasor.core.Hasor;
import net.hasor.dataway.dal.mapper.api.DwInterfaceHistoryMapper;
import net.hasor.dataway.dal.mapper.api.DwInterfaceInfoMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthRoleMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthUserMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsConfigMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsMapper;
import net.hasor.dbvisitor.hasor.autoconfig.DefaultDataSource;
import net.hasor.dbvisitor.session.Session;
import javax.sql.DataSource;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DwDalConfigTest {
    @Test
    public void shouldConfigureDbVisitorAndDatawayMappers() throws Exception {
        AppContext context = Hasor.create()
                .addSettings(Settings.DefaultNameSpace, "hasor.loadPackages", "net.hasor.dataway.dal")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.jdbc-url", "jdbc:h2:mem:dataway_dao;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.username", "sa")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.password", "")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.connection-timeout", "10000")
                .build();
        try {
            HikariDataSource dataSource = (HikariDataSource) context.getInstance(DataSource.class);
            DefaultDataSource driverDataSource = (DefaultDataSource) dataSource.getDataSource();
            assertEquals("com.mysql.cj.jdbc.Driver", driverDataSource.getDriverClassName());
            assertEquals("jdbc:h2:mem:dataway_dao;MODE=MySQL;DB_CLOSE_DELAY=-1", driverDataSource.getJdbcUrl());
            assertEquals("sa", driverDataSource.getUsername());
            assertEquals(10000L, dataSource.getConnectionTimeout());
            assertEquals(1, dataSource.getMinimumIdle());
            assertEquals(20, dataSource.getMaximumPoolSize());
            assertSame(context.getInstance(DataSource.class), context.getInstance(Session.class).getDataSource());
            assertNotNull(context.getInstance(Session.class));
            assertNotNull(context.getInstance(DwInterfaceInfoMapper.class));
            assertNotNull(context.getInstance(DwInterfaceHistoryMapper.class));
            assertNotNull(context.getInstance(DwDsMapper.class));
            assertNotNull(context.getInstance(DwDsConfigMapper.class));
            assertNotNull(context.getInstance(DwAuthUserMapper.class));
            assertNotNull(context.getInstance(DwAuthRoleMapper.class));
            assertTrue(context.getInstance(Session.class).jdbc()
                    .queryForInt("select count(*) from \"" + DwDbMigration.HISTORY_TABLE + "\"") > 0);
        } finally {
            context.shutdown();
        }
    }
}
