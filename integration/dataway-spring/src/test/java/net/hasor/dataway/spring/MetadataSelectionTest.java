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
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.spi.CallContext;
import net.hasor.dataway.spi.DatawayConfigurer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class MetadataSelectionTest {
    @Test
    void namedAndPrimaryAccessLayersUseContainerSelection() {
        var beans = new DefaultListableBeanFactory();
        var first = new JdbcDataAccessLayer(TestDatabase.create(), "");
        var second = new JdbcDataAccessLayer(TestDatabase.create(), "");
        beans.registerSingleton("first", first);
        var definition = new RootBeanDefinition(ApiDataAccessLayer.class, () -> second);
        definition.setPrimary(true);
        beans.registerBeanDefinition("second", definition);
        var environment = new MockEnvironment();
        var primary = this.assemble(beans, environment);
        primary.getService().save(new net.hasor.dataway.service.model.ApiDefinition("primary", "GET", "/primary", net.hasor.dataway.service.model.ScriptType.DATAQL, "return 1;", ""), 0, CallContext.LOCAL);
        environment.setProperty("dataway.metadata.bean", "first");
        assertTrue(this.assemble(beans, environment).getService().list(CallContext.LOCAL).isEmpty());
        environment.setProperty("dataway.metadata.bean", "second");
        assertEquals(1, this.assemble(beans, environment).getService().list(CallContext.LOCAL).size());
    }

    @Test
    void missingAmbiguousAndWrongNamedBeansFailWithoutFallback() {
        var beans = new DefaultListableBeanFactory();
        var environment = new MockEnvironment();
        assertThrows(NoSuchBeanDefinitionException.class, () -> this.assemble(beans, environment));
        beans.registerSingleton("first", new JdbcDataAccessLayer(TestDatabase.create(), ""));
        beans.registerSingleton("second", new JdbcDataAccessLayer(TestDatabase.create(), ""));
        assertThrows(NoUniqueBeanDefinitionException.class, () -> this.assemble(beans, environment));
        environment.setProperty("dataway.metadata.bean", "missing");
        assertThrows(NoSuchBeanDefinitionException.class, () -> this.assemble(beans, environment));
        beans.registerSingleton("wrong", "not a storage layer");
        environment.setProperty("dataway.metadata.bean", "wrong");
        assertThrows(BeanNotOfRequiredTypeException.class, () -> this.assemble(beans, environment));
    }

    @Test
    void blankNameUsesTypeAndExplicitBuilderAccessBypassesLookup() {
        var beans = new DefaultListableBeanFactory();
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        beans.registerSingleton("custom", access);
        var environment = new MockEnvironment().withProperty("dataway.metadata.bean", "  ");
        assertTrue(this.assemble(beans, environment).getService().list(CallContext.LOCAL).isEmpty());
        beans.destroySingletons();
        environment.setProperty("dataway.metadata.bean", "missing");
        beans.registerSingleton("customizer", (DatawayConfigurer) builder -> builder.dataAccessLayer(access));
        assertTrue(this.assemble(beans, environment).getService().list(CallContext.LOCAL).isEmpty());
    }

    private Dataway assemble(DefaultListableBeanFactory beans, MockEnvironment environment) {
        return new DatawayAutoConfiguration().dataway(beans.getBeanProvider(DataSource.class), beans, beans.getBeanProvider(DatawayConfigurer.class), environment);
    }
}
