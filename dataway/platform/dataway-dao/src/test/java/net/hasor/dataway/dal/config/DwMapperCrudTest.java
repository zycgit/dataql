/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.config;

import java.util.Date;
import net.hasor.cobble.setting.Settings;
import net.hasor.core.AppContext;
import net.hasor.core.Hasor;
import net.hasor.dataway.dal.mapper.api.DwInterfaceHistoryMapper;
import net.hasor.dataway.dal.mapper.api.DwInterfaceInfoMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthRoleMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthUserMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsConfigMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsMapper;
import net.hasor.dataway.dal.model.api.DwApiHistoryDO;
import net.hasor.dataway.dal.model.api.DwApiInfoDO;
import net.hasor.dataway.dal.model.auth.DwAuthRoleDO;
import net.hasor.dataway.dal.model.auth.DwAuthUserDO;
import net.hasor.dataway.dal.model.datasource.DwDsConfigDO;
import net.hasor.dataway.dal.model.datasource.DwDsDO;
import net.hasor.dbvisitor.session.Session;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DwMapperCrudTest {
    @Test
    public void shouldCrudAuthRole() throws Exception {
        AppContext context = this.createContext("auth_role");
        try {
            DwAuthRoleMapper mapper = context.getInstance(DwAuthRoleMapper.class);

            Date now = new Date();
            DwAuthRoleDO role = new DwAuthRoleDO();
            role.setGmtCreate(now);
            role.setGmtModified(now);
            role.setRoleName("ADMIN");
            role.setRoleAuthLabels("dataway:all");
            role.setAliasName("Administrator");
            role.setInnerTag(true);

            assertEquals(1, mapper.insert(role));
            assertNotNull(role.getId());
            assertEquals("ADMIN", mapper.selectById(role.getId()).getRoleName());

            role.setAliasName("Dataway Administrator");
            assertEquals(1, mapper.update(role));
            assertEquals("Dataway Administrator", mapper.selectById(role.getId()).getAliasName());

            assertEquals(1, mapper.deleteById(role.getId()));
            assertNull(mapper.selectById(role.getId()));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void shouldCrudAuthUser() throws Exception {
        AppContext context = this.createContext("auth_user");
        try {
            DwAuthUserMapper mapper = context.getInstance(DwAuthUserMapper.class);

            Date now = new Date();
            DwAuthUserDO user = new DwAuthUserDO();
            user.setGmtCreate(now);
            user.setGmtModified(now);
            user.setUid("user-001");
            user.setUsername("dataway");
            user.setEmail("dataway@hasor.net");
            user.setAccount("dataway");
            user.setPassword("encoded-password");
            user.setRoleId(1L);

            assertEquals(1, mapper.insert(user));
            assertNotNull(user.getId());
            assertEquals("dataway", mapper.selectById(user.getId()).getUsername());

            user.setEmail("admin@hasor.net");
            assertEquals(1, mapper.update(user));
            assertEquals("admin@hasor.net", mapper.selectById(user.getId()).getEmail());

            assertEquals(1, mapper.deleteById(user.getId()));
            assertNull(mapper.selectById(user.getId()));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void shouldCrudDataSource() throws Exception {
        AppContext context = this.createContext("data_source");
        try {
            DwDsMapper mapper = context.getInstance(DwDsMapper.class);

            Date now = new Date();
            DwDsDO dataSource = new DwDsDO();
            dataSource.setGmtCreate(now);
            dataSource.setGmtModified(now);
            dataSource.setDsType("MYSQL");
            dataSource.setName("dataway-main");
            dataSource.setDesc("Dataway database");
            dataSource.setDisplayHost("127.0.0.1:3306");
            dataSource.setDsEnvId(10L);
            dataSource.setDbVersion("8.0");
            dataSource.setDriverVersion("8.0.33");

            assertEquals(1, mapper.insert(dataSource));
            assertNotNull(dataSource.getId());
            assertEquals("MYSQL", mapper.selectById(dataSource.getId()).getDsType());

            dataSource.setDisplayHost("mysql.internal:3306");
            assertEquals(1, mapper.update(dataSource));
            assertEquals("mysql.internal:3306", mapper.selectById(dataSource.getId()).getDisplayHost());

            assertEquals(1, mapper.deleteById(dataSource.getId()));
            assertNull(mapper.selectById(dataSource.getId()));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void shouldCrudDataSourceConfig() throws Exception {
        AppContext context = this.createContext("ds_config");
        try {
            DwDsConfigMapper mapper = context.getInstance(DwDsConfigMapper.class);

            Date now = new Date();
            DwDsConfigDO config = new DwDsConfigDO();
            config.setGmtCreate(now);
            config.setGmtModified(now);
            config.setDsId(100L);
            config.setConfigName("jdbcUrl");
            config.setConfigValue("jdbc:mysql://localhost/dataway");

            assertEquals(1, mapper.insert(config));
            assertNotNull(config.getId());
            assertEquals("jdbcUrl", mapper.selectById(config.getId()).getConfigName());

            config.setConfigValue("jdbc:mysql://localhost/dataway_test");
            assertEquals(1, mapper.update(config));
            assertEquals("jdbc:mysql://localhost/dataway_test", mapper.selectById(config.getId()).getConfigValue());

            assertEquals(1, mapper.deleteById(config.getId()));
            assertNull(mapper.selectById(config.getId()));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void shouldCrudApiInfo() throws Exception {
        AppContext context = this.createContext("api_info");
        try {
            DwInterfaceInfoMapper mapper = context.getInstance(DwInterfaceInfoMapper.class);

            Date now = new Date();
            long currentTime = now.getTime();
            DwApiInfoDO api = new DwApiInfoDO();
            api.setGmtCreate(now);
            api.setGmtModified(now);
            api.setApiId("api-001");
            api.setApiMethod("GET");
            api.setApiPath("/users");
            api.setApiStatus("0");
            api.setApiComment("Query users");
            api.setApiType("DataQL");
            api.setApiScript("return [];");
            api.setApiSchema("{}");
            api.setApiSample("{}");
            api.setApiOption("{}");
            api.setApiCreateTime(currentTime);
            api.setApiGmtTime(currentTime);

            assertEquals(1, mapper.insert(api));
            assertNotNull(api.getId());
            assertEquals("/users", mapper.selectById(api.getId()).getApiPath());

            api.setApiStatus("1");
            assertEquals(1, mapper.update(api));
            assertEquals("1", mapper.selectById(api.getId()).getApiStatus());

            assertEquals(1, mapper.deleteById(api.getId()));
            assertNull(mapper.selectById(api.getId()));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void shouldCrudApiHistory() throws Exception {
        AppContext context = this.createContext("api_history");
        try {
            DwInterfaceHistoryMapper mapper = context.getInstance(DwInterfaceHistoryMapper.class);

            DwApiHistoryDO history = new DwApiHistoryDO();
            history.setHistoryId("history-001");
            history.setHistoryApiId("api-001");
            history.setHistoryMethod("GET");
            history.setHistoryPath("/users");
            history.setHistoryStatus("0");
            history.setHistoryComment("Initial version");
            history.setHistoryType("DataQL");
            history.setHistoryScript("return [];");
            history.setHistoryScriptOri("return [];");
            history.setHistorySchema("{}");
            history.setHistorySample("{}");
            history.setHistoryOption("{}");
            history.setHistoryCreateTime(System.currentTimeMillis());
            history.setRelease(false);

            assertEquals(1, mapper.insert(history));
            assertEquals("/users", mapper.selectById(history.getHistoryId()).getHistoryPath());

            history.setRelease(true);
            assertEquals(1, mapper.update(history));
            assertEquals(Boolean.TRUE, mapper.selectById(history.getHistoryId()).getRelease());

            assertEquals(1, mapper.deleteById(history.getHistoryId()));
            assertNull(mapper.selectById(history.getHistoryId()));
        } finally {
            context.shutdown();
        }
    }

    private AppContext createContext(String databaseName) {
        return Hasor.create()
                .addSettings(Settings.DefaultNameSpace, "hasor.loadPackages", "net.hasor.dataway.dal")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.jdbc-url", "jdbc:h2:mem:dataway_" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.username", "sa")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.password", "")
                .addSettings(Settings.DefaultNameSpace, "dataway.datasource.connection-timeout", "10000")
                .build();
    }
}
