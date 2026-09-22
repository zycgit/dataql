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
import net.hasor.dataway.Dataway;
import net.hasor.dataway.dal.*;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.dal.jdbc.LocalJdbcExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class MetadataSelectionTest {
    @Test
    void namedAndPrimaryBeansAreResolvedWithoutCreatingNewResources() {
        var beans = new DefaultListableBeanFactory();
        var first = TestDatabase.create();
        var second = TestDatabase.create();
        beans.registerSingleton("first", first);
        var definition = new RootBeanDefinition(DataSource.class, () -> second);
        definition.setPrimary(true);
        beans.registerBeanDefinition("second", definition);
        var context = new SpringMetadataContext(beans, new MockEnvironment());
        assertSame(first, context.getBean("first", DataSource.class));
        assertSame(second, context.getBean("", DataSource.class));
        assertThrows(org.springframework.beans.factory.NoSuchBeanDefinitionException.class, () -> context.getBean("missing", DataSource.class));
    }

    @Test
    void ambiguousResourcesFailRatherThanSelectingArbitrarily() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("first", TestDatabase.create());
        beans.registerSingleton("second", TestDatabase.create());
        var context = new SpringMetadataContext(beans, new MockEnvironment().withProperty("dataway.metadata.type", "jdbc"));
        assertThrows(org.springframework.beans.factory.NoUniqueBeanDefinitionException.class, () -> MetadataLoader.create(context));
    }

    @Test
    void explicitAccessAndExistingBeanTakePriorityOverConfiguredProvider() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("custom", access);
        var context = new SpringMetadataContext(beans, new MockEnvironment().withProperty("dataway.metadata.type", "missing"));
        assertSame(access, MetadataLoader.create(context));
        beans.destroySingletons();
        var dataway = Dataway.builder().dataAccessLayer(access).metadataContext(context).build();
        assertTrue(dataway.getService().list(net.hasor.dataway.spi.CallContext.LOCAL).isEmpty());
    }

    @Test
    void missingSelectionAndMissingProviderFailWithActionableMessages() {
        var environment = new MockEnvironment();
        var context = new SpringMetadataContext(new DefaultListableBeanFactory(), environment);
        assertTrue(assertThrows(IllegalStateException.class, () -> MetadataLoader.create(context)).getMessage().contains("dataway.metadata.type"));
        environment.setProperty("dataway.metadata.type", "absent");
        assertTrue(assertThrows(IllegalStateException.class, () -> MetadataLoader.create(context)).getMessage().contains("No metadata provider 'absent'"));
        environment.setProperty("dataway.metadata.bean", "missing");
        assertThrows(org.springframework.beans.factory.NoSuchBeanDefinitionException.class, () -> MetadataLoader.create(context));
    }

    @Test
    void suppliedExecutorIsUsedWithoutRequiringDatasourceOrTransactionManager() {
        var beans = new DefaultListableBeanFactory();
        var executor = new LocalJdbcExecutor(TestDatabase.create());
        beans.registerSingleton("hostExecutor", executor);
        var context = new SpringMetadataContext(beans, new MockEnvironment().withProperty("dataway.metadata.type", "jdbc").withProperty("dataway.metadata.jdbc.executor", "hostExecutor").withProperty("dataway.metadata.jdbc.data-source", "unused"));
        assertTrue(MetadataLoader.create(context).listObjects(EntityType.INFO, Map.of()).isEmpty());
    }

    @Test
    void deferredAccessPreservesTheCustomMutationFactory() {
        var mutation = new DataMutation() {
        };
        var access = new ApiDataAccessLayer() {
            public java.util.List<Map<FieldDef, String>> listObjects(EntityType type, Map<FieldDef, String> conditions) {
                return java.util.List.of();
            }

            public void write(java.util.List<DataMutation> mutations) {
            }

            public DataMutation create() {
                return mutation;
            }
        };
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var deferred = new DeferredDataAccessLayer(() -> {
            calls.incrementAndGet();
            return access;
        });
        assertSame(mutation, deferred.create());
        deferred.initialize();
        assertEquals(1, calls.get());
    }
}
