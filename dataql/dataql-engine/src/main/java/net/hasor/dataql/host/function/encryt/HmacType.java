/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.encryt;
/**
 * Hmac算法类型枚举
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-03-31
 */
public enum HmacType {
    HmacMD5("HmacMD5"),         //
    HmacSHA1("HmacSHA1"),       //
    HmacSHA256("HmacSHA256"),   //
    HmacSHA512("HmacSHA512");   //
    private final String hmacType;

    HmacType(String hmacType) {
        this.hmacType = hmacType;
    }

    public String getHmacType() {
        return hmacType;
    }

    public static HmacType formString(String hmacType) {
        for (HmacType digestType : HmacType.values()) {
            if (digestType.name().equalsIgnoreCase(hmacType)) {
                return digestType;
            }
        }
        return null;
    }
}
