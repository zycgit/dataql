/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.basic;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * 数学函数。函数库引入 <code>import 'net.hasor.dataql.fx.basic.NumberUdfSource' as number;</code>
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
public class NumberUdfSource extends AbstractUdfSource {
    public static int inRange(int value, int min, int max) {
        if (Math.min(value, min) == value) {
            return min;
        } else if (Math.max(value, max) == value) {
            return max;
        } else {
            return value;
        }
    }
}
