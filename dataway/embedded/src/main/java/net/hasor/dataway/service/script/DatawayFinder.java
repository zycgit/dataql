/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.FragmentProcess;

/** Overrides loaders while preserving the host Finder's bean and fragment resolution. */
final class DatawayFinder implements Finder {
    private final Finder         finder;
    private final ResourceLoader resourceLoader;
    private final ClassLoader    classLoader;

    public DatawayFinder(Finder finder, ResourceLoader resourceLoader, ClassLoader classLoader) {
        this.finder = finder;
        this.resourceLoader = resourceLoader != null ? resourceLoader : finder.getResourceLoader();
        this.classLoader = classLoader != null ? classLoader : finder.getClassLoader();
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public Object findBean(String beanName) throws ClassNotFoundException {
        return this.finder.findBean(beanName);
    }

    @Override
    public Object findBean(Class<?> beanType) {
        return this.finder.findBean(beanType);
    }

    @Override
    public FragmentProcess findFragmentProcess(String fragmentType) {
        return this.finder.findFragmentProcess(fragmentType);
    }
}
