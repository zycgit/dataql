/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;
import java.util.Objects;
import java.util.function.Supplier;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.host.spi.SpiRegistry;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.FragmentProcess;

/**
 * 宿主配置中心，负责准备资源加载、类加载以及 SPI 扩展。
 */
public class HostConfiguration implements HostContext {
    private final ResourceLoader resourceLoader;
    private final ClassLoader    classLoader;
    private final Finder         parent;
    private final SpiRegistry    spiRegistry;

    public HostConfiguration() {
        this(null);
    }

    public HostConfiguration(Finder parent) {
        this(parent, resolveResourceLoader(parent, null), resolveClassLoader(parent, null));
    }

    public HostConfiguration(ResourceLoader resourceLoader, ClassLoader classLoader) {
        this(null, resolveResourceLoader(null, resourceLoader), resolveClassLoader(null, classLoader));
    }

    private HostConfiguration(Finder parent, ResourceLoader resourceLoader, ClassLoader classLoader) {
        this.parent = parent;
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader is null.");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader is null.");
        this.spiRegistry = new SpiRegistry(this);
    }

    private static ResourceLoader resolveResourceLoader(Finder parent, ResourceLoader resourceLoader) {
        if (parent != null) {
            return parent.getResourceLoader();
        } else {
            return resourceLoader != null ? resourceLoader : ClassPathResourceLoader.INSTANCE;
        }
    }

    private static ClassLoader resolveClassLoader(Finder parent, ClassLoader classLoader) {
        if (parent != null) {
            return parent.getClassLoader();
        } else {
            return classLoader != null ? classLoader : HostConfiguration.class.getClassLoader();
        }
    }

    //

    public HostContext getHostContext() {
        return this;
    }

    public Finder getParent() {
        return this.parent;
    }

    @Override
    public <T> T getAttachment(Class<T> attachmentType) {
        return this.spiRegistry.getAttachment(attachmentType);
    }

    @Override
    public <T> void addAttachment(Class<T> attachmentType, T attachment) {
        this.spiRegistry.addAttachment(attachmentType, attachment);
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader != null ? this.classLoader : Thread.currentThread().getContextClassLoader();
    }

    public void addFragment(String name, Supplier<? extends FragmentProcess> provider) {
        this.spiRegistry.addFragment(name, provider);
    }

    public void addImport(String name, Supplier<?> provider) {
        this.spiRegistry.addImport(name, provider);
    }

    @Override
    public Object findBean(String beanName) throws ClassNotFoundException {
        Object bean = this.spiRegistry.findImport(beanName);
        if (bean != null) {
            return bean;
        }
        if (this.parent != null) {
            return this.parent.findBean(beanName);
        }
        ClassLoader loader = this.getClassLoader();
        return this.findBean(loader != null ? loader.loadClass(beanName) : Class.forName(beanName));
    }

    @Override
    public Object findBean(Class<?> beanType) {
        Object bean = this.spiRegistry.findImport(beanType.getName());
        if (bean != null) {
            return bean;
        }
        if (this.parent != null) {
            return this.parent.findBean(beanType);
        }
        return ClassUtils.newInstance(beanType);
    }

    @Override
    public FragmentProcess findFragmentProcess(String fragmentType) {
        FragmentProcess process = this.spiRegistry.findFragment(fragmentType);
        if (process != null) {
            return process;
        }
        if (this.parent != null) {
            return this.parent.findFragmentProcess(fragmentType);
        }
        throw new UnsupportedOperationException(fragmentType + " fragment undefine.");
    }
}
