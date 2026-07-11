/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
package net.hasor.dataql.kernel;

import net.hasor.cobble.loader.ResourceLoader;

/** 运行期资源查找器。 */
public interface Finder {
    /** 获取类加载器。 */
    ClassLoader getClassLoader();

    /** 获取脚本资源加载器。 */
    ResourceLoader getResourceLoader();

    Object findBean(String beanName) throws ClassNotFoundException;

    Object findBean(Class<?> beanType);

    FragmentProcess findFragmentProcess(String fragmentType);
}
