/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.Map;

/**
 * 用户自定义数据取值作用域
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-11
 */
public interface CustomizeScope {
    /** 自定义取值，操作符将下面下之一：#、@、$(常用) */
    Map<String, ?> findCustomizeEnvironment(String symbol);
}
