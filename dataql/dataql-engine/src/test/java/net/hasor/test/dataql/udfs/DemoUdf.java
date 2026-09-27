/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.dataql.udfs;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.UdfParams;

public class DemoUdf implements Udf {
    @Override
    public Object call(Hints readOnly, UdfParams params) {
        return new DataBean();
    }
}
