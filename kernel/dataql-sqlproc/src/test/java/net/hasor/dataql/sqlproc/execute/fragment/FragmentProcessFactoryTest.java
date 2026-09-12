/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.FragmentProcessFactory;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.sqlproc.execute.support.SqlQueryContextFactory;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FragmentProcessFactoryTest {
    @Test
    public void queryContextIsScopedToHost() {
        TestHostContext firstHost = new TestHostContext();
        TestHostContext secondHost = new TestHostContext();
        SqlQueryContextFactory factory = new SqlQueryContextFactory();

        firstHost.addAttachment(ExecuteContext.class, factory.create(firstHost));
        secondHost.addAttachment(ExecuteContext.class, factory.create(secondHost));

        assertSame(firstHost.getAttachment(ExecuteContext.class), firstHost.getAttachment(ExecuteContext.class));
        assertNotSame(firstHost.getAttachment(ExecuteContext.class), secondHost.getAttachment(ExecuteContext.class));
    }

    @Test
    public void discoversAllNamedFactories() {
        Map<String, FragmentProcessFactory> factories = new HashMap<>();
        AtomicInteger providerCount = new AtomicInteger();
        ServiceLoader.load(FragmentProcessFactory.class).forEach(factory -> {
            providerCount.incrementAndGet();
            for (String name : factory.getNames()) {
                factories.put(name, factory);
            }
        });

        assertEquals(6, providerCount.get());
        assertEquals(12, factories.size());
        for (String operation : new String[] { "execute", "select", "insert", "update", "delete", "call" }) {
            assertTrue(factories.containsKey(operation + "Sql"));
            assertTrue(factories.containsKey(operation + "Xml"));
        }

        TestHostContext hostConfiguration = new TestHostContext();
        hostConfiguration.addAttachment(ExecuteContext.class, new SqlQueryContextFactory().create(hostConfiguration));
        FragmentProcessFactory selectFactory = factories.get("selectSql");
        assertSame(selectFactory, factories.get("selectXml"));
        FragmentProcess first = selectFactory.create("selectSql", hostConfiguration);
        FragmentProcess second = selectFactory.create("selectXml", hostConfiguration);
        assertNotSame(first, second);
    }

    private static class TestHostContext implements HostContext {
        private final Map<Class<?>, Object> attachments = new HashMap<>();

        @Override
        public <T> T getAttachment(Class<T> attachmentType) {
            return attachmentType.cast(this.attachments.get(attachmentType));
        }

        @Override
        public <T> void addAttachment(Class<T> attachmentType, T attachment) {
            this.attachments.put(attachmentType, attachment);
        }

        @Override
        public ClassLoader getClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override
        public ResourceLoader getResourceLoader() {
            return ClassPathResourceLoader.INSTANCE;
        }

        @Override
        public Object findBean(String beanName) throws ClassNotFoundException {
            return this.findBean(this.getClassLoader().loadClass(beanName));
        }

        @Override
        public Object findBean(Class<?> beanType) {
            try {
                return beanType.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public FragmentProcess findFragmentProcess(String fragmentType) {
            throw new UnsupportedOperationException(fragmentType + " fragment undefine.");
        }
    }
}
