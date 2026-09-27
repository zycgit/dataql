/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;

/**
 * UDF
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
@FunctionalInterface
public interface Udf {
    /** UDF 的返回值必须是一个 对象或者数组 */
    Object call(Hints readOnly, UdfParams params) throws Throwable;
}
