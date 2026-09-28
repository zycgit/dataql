/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.DatawayConfig;
import net.hasor.dataway.service.DatawayFinder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class FinderConfigurationTest {
    private final WebApplicationContextRunner context = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DatawayAutoConfiguration.class, WebMvcAutoConfiguration.class)).withPropertyValues("dataway.api-enabled=true");

    @Test
    void replacingDefaultFinderSwitchesLoadersAndKeepsDefaultBeanResolution() throws Exception {
        try (var oldLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader()); var newLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader())) {
            ResourceLoader oldResources = new ClassPathResourceLoader(oldLoader);
            ResourceLoader newResources = new ClassPathResourceLoader(newLoader);
            DatawayConfig config = new DatawayConfig().dataAccessLayer(TestDatabase.dataAccessLayer());
            DatawayFinder original = assertInstanceOf(DatawayFinder.class, config.getFinder());
            config.resourceLoader(oldResources).classLoader(oldLoader);
            assertSame(oldResources, original.getResourceLoader());
            assertSame(oldLoader, original.getClassLoader());

            DatawayFinder replacement = new DatawayFinder();
            replacement.setResourceLoader(newResources);
            replacement.setClassLoader(newLoader);
            config.finder(replacement);
            assertSame(replacement, config.getFinder());
            assertSame(newResources, config.getResourceLoader());
            assertSame(newLoader, config.getClassLoader());
            config.configureHost(host -> {
                assertSame(replacement, host.getParent());
                assertSame(newResources, host.getResourceLoader());
                assertSame(newLoader, host.getClassLoader());
                assertInstanceOf(HashMap.class, host.findBean(HashMap.class));
                assertInstanceOf(ArrayList.class, assertDoesNotThrow(() -> host.findBean(ArrayList.class.getName())));
            });
            this.context.withBean(DatawayConfig.class, () -> config).run(c -> {
                assertNull(c.getStartupFailure());
                assertNotNull(c.getBean(Dataway.class).getApiHandler());
            });
        }
    }

    @Test
    void loaderShortcutsConfigureTheReplacementFinder() throws Exception {
        try (var loader = new URLClassLoader(new URL[0], this.getClass().getClassLoader())) {
            ResourceLoader resources = new ClassPathResourceLoader(loader);
            DatawayFinder replacement = new DatawayFinder();
            DatawayConfig config = new DatawayConfig().finder(replacement).resourceLoader(resources).classLoader(loader).dataAccessLayer(TestDatabase.dataAccessLayer());
            assertSame(resources, replacement.getResourceLoader());
            assertSame(loader, replacement.getClassLoader());
            config.configureHost(host -> {
                assertSame(config.getResourceLoader(), host.getResourceLoader());
                assertSame(config.getClassLoader(), host.getClassLoader());
            });
            this.context.withBean(DatawayConfig.class, () -> config).run(c -> assertNull(c.getStartupFailure()));
        }
    }

    @Test
    void nativeFinderSuppliesItsOwnLoadersAndImportsToPublishedQueries() throws Exception {
        try (var oldLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader()); var newLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader())) {
            ResourceLoader oldResources = new ClassPathResourceLoader(oldLoader);
            ResourceLoader newResources = new ClassPathResourceLoader(newLoader);
            HostConfiguration finder = new HostConfiguration(newResources, newLoader);
            finder.addImport("app.value", () -> (Udf) (hints, parameters) -> "host finder");
            DatawayConfig config = new DatawayConfig().resourceLoader(oldResources).classLoader(oldLoader).finder(finder).dataAccessLayer(TestDatabase.dataAccessLayer()).resultStructure(false);
            assertSame(newResources, config.getResourceLoader());
            assertSame(newLoader, config.getClassLoader());
            assertThrows(IllegalStateException.class, () -> config.resourceLoader(oldResources));
            assertThrows(IllegalStateException.class, () -> config.classLoader(oldLoader));
            config.configureHost(host -> {
                assertSame(finder, host.getParent());
                assertSame(newResources, host.getResourceLoader());
                assertSame(newLoader, host.getClassLoader());
            });
            this.context.withBean(DatawayConfig.class, () -> config).run(c -> {
                assertNull(c.getStartupFailure());
                var admin = c.getBean(Dataway.class).getAdminService();
                ApiDefinition definition = new ApiDefinition();
                definition.setId("finder");
                definition.setMethod("GET");
                definition.setPath("/finder");
                definition.setType(ApiScriptType.DATA_QL);
                definition.setScript("import 'app.value' as value; return value();");
                definition.setDescription("");
                admin.save(definition, 0);
                admin.publish("finder", 1);
                var mvc = MockMvcBuilders.webAppContextSetup(c.getSourceApplicationContext()).build();
                var response = mvc.perform(get("/api/finder")).andReturn().getResponse();
                assertEquals(200, response.getStatus());
                assertEquals("\"host finder\"", response.getContentAsString());
            });
        }
    }
}
