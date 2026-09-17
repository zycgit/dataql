/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.dataql.udfs;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.hasor.dataql.host.function.AbstractUdfSource;

//@DimUdfSource("time")
public class TimeUdfSource extends AbstractUdfSource {
    /** 格式化指定时间 */
    public String format(long time, String pattern) {
        return new SimpleDateFormat(pattern).format(new Date(time));
    }

    /** 格式化为：yyyy-MM-dd HH:mm:ss */
    public String ymd_hms(Object time) {
        if (time == null) {
            return null;
        }
        return format(Long.parseLong(time.toString()), "yyyy-MM-dd HH:mm:ss");
    }

    /** 格式化为：yyyy-MM-dd */
    public String ymd(Object time) {
        if (time == null) {
            return null;
        }
        return format(Long.parseLong(time.toString()), "yyyy-MM-dd");
    }

    /** 格式化为：HH:mm:ss */
    public String hms(Object time) {
        if (time == null) {
            return null;
        }
        return format(Long.parseLong(time.toString()), "HH:mm:ss");
    }
}
