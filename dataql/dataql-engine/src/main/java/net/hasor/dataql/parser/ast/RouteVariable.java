/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.parser.ast;
/**
 * 路由变量
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-11-07
 */
public interface RouteVariable extends Variable {
    RouteVariable getParent();
}
