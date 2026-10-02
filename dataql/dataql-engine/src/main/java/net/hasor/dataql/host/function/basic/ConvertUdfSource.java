/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.basic;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.hasor.cobble.BooleanUtils;
import net.hasor.cobble.NumberUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.codec.HexUtils;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataql.domain.BinaryValue;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * 转换函数。函数库引入 <code>import 'net.hasor.dataql.host.function.basic.ConvertUdfSource' as convert;</code>
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
    public static String byteToHex(Object content) throws IOException {
        if (content == null) {
            return null;
        }
        return HexUtils.bytes2hex(readBytes(content));
    }

    /** 将16进制字符串转换为二进制数据 */
    public static BinaryValue hexToByte(String content) {
        if (content == null) {
            return null;
        }
        if (content.isEmpty()) {
            return new BinaryValue(new byte[0]);
        }

        return new BinaryValue(HexUtils.hex2bytes(content));
    }

    /** 二进制数据转换为字符串，未指定字符集时使用 UTF-8。 */
    public static String byteToString(Object content, String charset) throws IOException {
        if (content == null) {
            return null;
        }
        Charset encoding = charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
        return new String(readBytes(content), encoding);
    }

    /** 文本转换为二进制值，未指定字符集时使用 UTF-8。 */
    public static BinaryValue textToByte(String content, String charset) {
        if (content == null) {
            return new BinaryValue(new byte[0]);
        }
        Charset encoding = charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
        return new BinaryValue(content.getBytes(encoding));
    }

    /** 保留现有字符串转换入口，返回统一的二进制值。 */
    public static BinaryValue stringToByte(String content, String charset) {
        return textToByte(content, charset);
    }

    private static byte[] readBytes(Object content) throws IOException {
        if (content instanceof BinaryModel binary) {
            try (var input = binary.openStream()) {
                return input.readAllBytes();
            }
        }
        if (content instanceof byte[] bytes) {
            return bytes;
        }
        if (content instanceof List<?> values) {
            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                if (!(values.get(i) instanceof Number value)) {
                    throw new IllegalArgumentException("Byte list elements must be numbers");
                }
                bytes[i] = value.byteValue();
            }
            return bytes;
        }
        throw new IllegalArgumentException("Expected binary content, a byte array or a byte list");
    }
}
