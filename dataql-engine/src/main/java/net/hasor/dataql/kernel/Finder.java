/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
 */
package net.hasor.dataql.kernel;

/**
 * 资源加载器
 */
public interface Finder {
    /** 默认实现 */

    Object findBean(String beanName) throws ClassNotFoundException;

    Object findBean(Class<?> beanType);

    FragmentProcess findFragmentProcess(String fragmentType);
}
