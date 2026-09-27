/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.util.Map;
import javax.sql.DataSource;
import net.hasor.dataway.authorization.Operation;
import net.hasor.dataway.authorization.UserIdentity;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayException;
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
import org.springframework.transaction.support.TransactionTemplate;
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
        Dataway dataway = Dataway.builder().dataAccessLayer(new JdbcDataAccessLayer(new SpringJdbcExecutor(new TransactionAwareDataSourceProxy(source), manager), "")).build();
        var transaction = new TransactionTemplate(manager);
        transaction.executeWithoutResult(status -> {
            dataway.getAdminService().save(api("one"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            status.setRollbackOnly();
        });
        assertTrue(dataway.getAdminService().list(Operation.LIST, UserIdentity.anonymous(), Map.of(), null).isEmpty());
    }

    private static AnnotationConfigApplicationContext createContext() {
        var context = new AnnotationConfigApplicationContext();
        context.register(HostConfig.class, DatawayAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static ApiDefinition api(String id) {
        ApiDefinition definition = new ApiDefinition();
        definition.setId(id);
        definition.setMethod("GET");
        definition.setPath("/one");
        definition.setType(ApiScriptType.DATAQL);
        definition.setScript("return 1;");
        definition.setDescription("");
        return definition;
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
        ApiDataAccessLayer metadataStore(DataSource source, PlatformTransactionManager manager) {
            return new JdbcDataAccessLayer(new SpringJdbcExecutor(source, manager), "");
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
            dataway.getAdminService().save(api("one"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            dataway.getAdminService().publish("one", 1, Operation.PUBLISH, UserIdentity.anonymous(), Map.of(), null);
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
            dataway.getAdminService().save(api("one"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            try {
                dataway.getAdminService().save(api("duplicate"), 0, Operation.SAVE, UserIdentity.anonymous(), Map.of(), null);
            } catch (DatawayException e) {
                assertEquals(409, e.status());
            }
        }
    }
}
