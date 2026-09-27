/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;

/**
 * 函数调用
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class UdfModel implements DataModel, Udf {
    private Udf udf = null;

    UdfModel(Udf udf) {
        this.udf = udf;
    }

    @Override
    public Udf asOri() {
        return this.udf;
    }

    @Override
    public Udf unwrap() {
        return this.udf;
    }

    /** 判断是否为 UdfModel 类型值 */
    public boolean isUdf() {
        return true;
    }

    @Override
    public DataModel call(Hints readOnly, UdfParams params) throws Throwable {
        return DomainHelper.convertTo(this.udf.call(readOnly, params));
    }
}
