/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import javax.sql.DataSource;
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.service.model.ApiDefinition;
import net.hasor.dataway.service.model.ScriptType;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

class SpringTransactionTest {
    @Test
    void nativeTransactionalRollbackIncludesDatawayDraftAndRelease() {
        try (var context = createContext()) {
            HostService host = context.getBean(HostService.class);
            assertThrows(IllegalStateException.class, host::rollback);
            var jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM host_work", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM interface_info", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM interface_release", Integer.class));
            host.commit();
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM host_work", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM interface_release", Integer.class));
        }
    }

    @Test
    void caughtBatchConflictMarksTheHostTransactionRollbackOnly() {
        try (var context = createContext()) {
            assertThrows(UnexpectedRollbackException.class, () -> context.getBean(HostService.class).catchConflict());
            var jdbc = new JdbcTemplate(context.getBean(DataSource.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM interface_info", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM host_work", Integer.class));
        }
    }

    @Test
    void transactionAwareDatasourceProxyUsesTheSameSpringResource() {
        DataSource source = TestDatabase.create();
        var manager = new JdbcTransactionManager(source);
        Dataway dataway = Dataway.builder().dataSource(new TransactionAwareDataSourceProxy(source)).dataAccessLayer(new JdbcDataAccessLayer(new SpringJdbcExecutor(new TransactionAwareDataSourceProxy(source), manager), "")).build();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
        transaction.executeWithoutResult(status -> {
            dataway.getService().save(api("one"), 0, CallContext.LOCAL);
            status.setRollbackOnly();
        });
        assertTrue(dataway.getService().list(CallContext.LOCAL).isEmpty());
    }

    private static AnnotationConfigApplicationContext createContext() {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("metadata", java.util.Map.of("dataway.metadata.type", "jdbc", "dataway.metadata.jdbc.data-source", "source", "dataway.metadata.jdbc.transaction-manager", "transactionManager")));
        context.register(HostConfig.class, DatawayAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static ApiDefinition api(String id) {
        return new ApiDefinition(id, "GET", "/one", ScriptType.DATAQL, "return 1;", "");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class HostConfig {
        @Bean
        DataSource source() {
            var source = TestDatabase.create();
            new JdbcTemplate(source).execute("CREATE TABLE host_work (id INT PRIMARY KEY)");
            return source;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new JdbcTransactionManager(source);
        }

        @Bean
        HostService hostService(Dataway dataway, DataSource source) {
            return new HostService(dataway, source);
        }
    }

    public static class HostService {
        private final Dataway      dataway;
        private final JdbcTemplate jdbc;

        public HostService(Dataway dataway, DataSource source) {
            this.dataway = dataway;
            this.jdbc = new JdbcTemplate(source);
        }

        private void saveAndPublish() {
            jdbc.update("INSERT INTO host_work VALUES (1)");
            dataway.getService().save(api("one"), 0, CallContext.LOCAL);
            dataway.getService().publish("one", 1, CallContext.LOCAL);
        }

        @Transactional
        public void rollback() {
            saveAndPublish();
            throw new IllegalStateException("host failure");
        }

        @Transactional
        public void commit() {
            saveAndPublish();
        }

        @Transactional
        public void catchConflict() {
            jdbc.update("INSERT INTO host_work VALUES (1)");
            dataway.getService().save(api("one"), 0, CallContext.LOCAL);
            try {
                dataway.getService().save(api("duplicate"), 0, CallContext.LOCAL);
            } catch (DatawayException e) {
                assertEquals(409, e.status());
            }
        }
    }
}
