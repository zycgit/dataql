/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.basic;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * 状态函数 <code>import 'net.hasor.dataql.fx.basic.StateUdfSource' as state;</code>
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
public class StateUdfSource extends AbstractUdfSource {
    /** 返回一个Udf，每次调用这个UDF，都会返回一个 Number。Number值较上一次会自增 1。 */
    public static Udf decNumber(long initValue) {
        AtomicLong atomicLong = new AtomicLong(initValue);
        return (params, readOnly) -> atomicLong.incrementAndGet();
    }

    /** 返回一个Udf，每次调用这个UDF，都会返回一个 Number。Number值较上一次会自减 1。 */
    public static Udf incNumber(long initValue) {
        AtomicLong atomicLong = new AtomicLong(initValue);
        return (params, readOnly) -> atomicLong.decrementAndGet();
    }

    /** 返回一个完整格式的 UUID 字符串。 */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** 返回一个不含"-" 符号的 UUID 字符串 */
    public static String uuidToShort() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
