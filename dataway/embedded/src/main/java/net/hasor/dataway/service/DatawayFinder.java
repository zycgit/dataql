/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.FragmentProcess;

/** Default Finder with configurable loaders and reflection-based bean creation. */
public class DatawayFinder implements Finder {
    private ResourceLoader resourceLoader = ClassPathResourceLoader.INSTANCE;
    private ClassLoader    classLoader    = DatawayFinder.class.getClassLoader();

    @Override
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : ClassPathResourceLoader.INSTANCE;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader != null ? classLoader : DatawayFinder.class.getClassLoader();
    }

    @Override
    public Object findBean(String beanName) throws ClassNotFoundException {
        return this.findBean(this.getClassLoader().loadClass(beanName));
    }

    @Override
    public Object findBean(Class<?> beanType) {
        return ClassUtils.newInstance(beanType);
    }

    @Override
    public FragmentProcess findFragmentProcess(String fragmentType) {
        throw new UnsupportedOperationException(fragmentType + " fragment undefine.");
    }
}
