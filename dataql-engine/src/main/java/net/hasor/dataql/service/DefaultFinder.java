/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.service;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.hasor.cobble.ClassUtils;
import net.hasor.cobble.ref.LinkedCaseInsensitiveMap;
import net.hasor.dataql.Finder;
import net.hasor.dataql.FragmentProcess;
import net.hasor.dataql.spi.FinderProvider;

/**
 * 资源加载器
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public class DefaultFinder implements Finder {
    private final Finder                   parent;
    private final ClassLoader              classLoader;
    private final Map<String, Supplier<?>> fragmentMap      = new LinkedCaseInsensitiveMap<>();
    private final Map<String, Supplier<?>> importPrepareMap = new ConcurrentHashMap<>();

    public DefaultFinder() {
        this(Thread.currentThread().getContextClassLoader(), null);
    }

    public DefaultFinder(ClassLoader classLoader, Finder parent) {
        this.classLoader = classLoader;
        this.parent = parent;
        this.loadFinderProviders();
    }

    private void loadFinderProviders() {
        ServiceLoader<FinderProvider> serviceLoader;
        if (this.classLoader != null) {
            serviceLoader = ServiceLoader.load(FinderProvider.class, this.classLoader);
        } else {
            serviceLoader = ServiceLoader.load(FinderProvider.class);
        }
        serviceLoader.forEach(provider -> provider.loadTo(this));
    }

    public Finder getParent() {
        return this.parent;
    }

    @Override
    public Object findBean(String beanName) throws ClassNotFoundException {
        Supplier<?> supplier = this.importPrepareMap.get(beanName);
        if (supplier != null) {
            return supplier.get();
        }
        if (this.parent != null) {
            return this.parent.findBean(beanName);
        }
        ClassLoader useClassLoader = this.classLoader != null ? this.classLoader : Thread.currentThread().getContextClassLoader();
        Class<?> beanType = useClassLoader != null ? useClassLoader.loadClass(beanName) : Class.forName(beanName);
        return this.findBean(beanType);
    }

    @Override
    public Object findBean(Class<?> beanType) {
        String typeName = beanType.getName();
        if (!this.importPrepareMap.containsKey(typeName)) {
            this.importPrepareMap.put(typeName, () -> {
                return this.parent != null ? this.parent.findBean(beanType) : ClassUtils.newInstance(beanType);
            });
        }
        return this.importPrepareMap.get(typeName).get();
    }

    @Override
    public FragmentProcess findFragmentProcess(String fragmentType) {
        Supplier<?> supplier = this.fragmentMap.get(fragmentType);
        FragmentProcess process = null;
        if (supplier != null) {
            process = (FragmentProcess) supplier.get();
        }
        if (process == null) {
            if (this.parent != null) {
                return this.parent.findFragmentProcess(fragmentType);
            }
            throw new UnsupportedOperationException(fragmentType + " fragment undefine.");
        }
        return process;
    }

    public void addImport(String name, Class<?> implementation) {
        this.importPrepareMap.put(name, () -> findBean(implementation));
    }

    public void addImport(String name, Supplier<?> provider) {
        this.importPrepareMap.put(name, provider);
    }

    public void addFragmentProcess(String name, Class<? extends FragmentProcess> implementation) {
        this.fragmentMap.put(name, () -> findBean(implementation));
    }

    public void addFragmentProcess(String name, Supplier<? extends FragmentProcess> provider) {
        this.fragmentMap.put(name, provider);
    }
}
