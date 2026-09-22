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
import net.hasor.dataway.dal.jdbc.LocalJdbcExecutor;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
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
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean("missing", DataSource.class));
    }

    @Test
    void ambiguousResourcesFailRatherThanSelectingArbitrarily() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("first", TestDatabase.create());
        beans.registerSingleton("second", TestDatabase.create());
        var environment = new MockEnvironment().withProperty("dataway.metadata.type", "jdbc");
        assertThrows(NoUniqueBeanDefinitionException.class, () -> this.assemble(beans, environment));
    }

    @Test
    void explicitAccessAndExistingBeanTakePriorityOverConfiguredProvider() {
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("custom", access);
        var environment = new MockEnvironment().withProperty("dataway.metadata.type", "missing");
        assertTrue(this.assemble(beans, environment).getService().list(CallContext.LOCAL).isEmpty());
        beans.destroySingletons();
        beans.registerSingleton("customizer", (DatawayConfigurer) builder -> builder.dataAccessLayer(access));
        var dataway = this.assemble(beans, environment);
        assertTrue(dataway.getService().list(net.hasor.dataway.spi.CallContext.LOCAL).isEmpty());
    }

    @Test
    void missingSelectionAndMissingProviderFailWithActionableMessages() {
        var environment = new MockEnvironment();
        var beans = new DefaultListableBeanFactory();
        assertTrue(assertThrows(IllegalStateException.class, () -> this.assemble(beans, environment)).getMessage().contains("dataway.metadata.type"));
        environment.setProperty("dataway.metadata.type", "absent");
        assertTrue(assertThrows(IllegalStateException.class, () -> this.assemble(beans, environment)).getMessage().contains("No metadata provider 'absent'"));
        environment.setProperty("dataway.metadata.bean", "missing");
        assertThrows(NoSuchBeanDefinitionException.class, () -> this.assemble(beans, environment));
    }

    @Test
    void suppliedExecutorIsUsedWithoutRequiringDatasourceOrTransactionManager() {
        var beans = new DefaultListableBeanFactory();
        var executor = new LocalJdbcExecutor(TestDatabase.create());
        beans.registerSingleton("hostExecutor", executor);
        var environment = new MockEnvironment().withProperty("dataway.metadata.type", "jdbc").withProperty("dataway.metadata.jdbc.executor", "hostExecutor").withProperty("dataway.metadata.jdbc.data-source", "unused");
        assertTrue(this.assemble(beans, environment).getService().list(CallContext.LOCAL).isEmpty());
    }

    private Dataway assemble(DefaultListableBeanFactory beans, MockEnvironment environment) {
        return new DatawayAutoConfiguration().dataway(          //
                beans.getBeanProvider(DataSource.class),        //
                beans,                                          //
                beans.getBeanProvider(DatawayConfigurer.class), //
                environment);
    }

}
