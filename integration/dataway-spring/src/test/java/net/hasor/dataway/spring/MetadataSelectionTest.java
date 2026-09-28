/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
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
        ApiDefinition primaryApi = new ApiDefinition();
        primaryApi.setId("primary");
        primaryApi.setMethod("GET");
        primaryApi.setPath("/primary");
        primaryApi.setType(ApiScriptType.DATA_QL);
        primaryApi.setScript("return 1;");
        primaryApi.setDescription("");
        primary.getAdminService().save(primaryApi, 0);
        environment.setProperty("dataway.metadata.bean", "first");
        assertTrue(this.assemble(beans, environment).getAdminService().list().isEmpty());
        environment.setProperty("dataway.metadata.bean", "second");
        assertEquals(1, this.assemble(beans, environment).getAdminService().list().size());
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
    void blankNameUsesTypeAndExplicitConfigurationAccessBypassesLookup() {
        var beans = new DefaultListableBeanFactory();
        var access = new JdbcDataAccessLayer(TestDatabase.create(), "");
        beans.registerSingleton("custom", access);
        var environment = new MockEnvironment().withProperty("dataway.metadata.bean", "  ");
        assertTrue(this.assemble(beans, environment).getAdminService().list().isEmpty());
        beans.destroySingletons();
        environment.setProperty("dataway.metadata.bean", "missing");
        beans.registerSingleton("customizer", new DatawayConfig().dataAccessLayer(access));
        assertTrue(this.assemble(beans, environment).getAdminService().list().isEmpty());
    }

    private Dataway assemble(DefaultListableBeanFactory beans, MockEnvironment environment) {
        return new DatawayAutoConfiguration().dataway(beans, beans.getBeanProvider(DatawayConfig.class), environment);
    }
}
