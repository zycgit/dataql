/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.basic;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import net.hasor.cobble.ArrayUtils;
import net.hasor.cobble.BooleanUtils;
import net.hasor.cobble.NumberUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.codec.HexUtils;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * 转换函数。函数库引入 <code>import 'net.hasor.dataql.fx.basic.ConvertUdfSource' as convert;</code>
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
public class ConvertUdfSource extends AbstractUdfSource {
    /** 将对象转换为 Number */
    public static Number toInt(Object target) {
        if (target instanceof Number) {
            return (Number) target;
        } else if (target == null) {
            return 0;
        } else if (target instanceof String) {
            if (StringUtils.isBlank((String) target)) {
                return 0;
            }
            return NumberUtils.createNumber((String) target);
        }
        return 0;
    }

    /** 将对象转换为 String */
    public static String toString(Object target) {
        return String.valueOf(target);
    }

    /** 将对象转换为 Boolean */
    public static Boolean toBoolean(Object target) {
        if (target instanceof Boolean) {
            return (Boolean) target;
        } else if (target instanceof String) {
            return BooleanUtils.toBooleanObject((String) target);
        } else {
            return Boolean.FALSE;
        }
    }

    /** 将二进制数据转换为16进制字符串 */
    public static String byteToHex(List<Byte> content) {
        if (content == null) {
            return null;
        }
        if (content.size() == 0) {
            return "";
        }
        Byte[] bytes = content.toArray(new Byte[0]);
        return HexUtils.bytes2hex(ArrayUtils.toPrimitive(bytes));
    }

    /** 将16进制字符串转换为二进制数据 */
    public static byte[] hexToByte(String content) {
        if (content == null) {
            return null;
        }
        if (content.equals("")) {
            return new byte[0];
        }
        return HexUtils.hex2bytes(content);
    }

    /** 二进制数据转换为字符串 */
    public static String byteToString(List<Byte> content, String charset) {
        if (content == null || content.size() == 0) {
            return null;
        }
        Byte[] bytes = content.toArray(new Byte[0]);
        return new String(ArrayUtils.toPrimitive(bytes), Charset.forName(charset));
    }

    /** 字符串转换为二进制数据 */
    public static byte[] stringToByte(String content, String charset) throws UnsupportedEncodingException {
        if (content == null || content.equals("")) {
            return new byte[0];
        }
        return content.getBytes(charset);
    }
}
