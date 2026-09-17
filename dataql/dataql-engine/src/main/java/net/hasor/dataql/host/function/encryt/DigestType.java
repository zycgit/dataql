/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.encryt;
/**
 * 摘要算法类型枚举
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-03-31
 */
public enum DigestType {
    MD5("MD5"),         //
    SHA("SHA"),         //
    SHA1("SHA1"),       //
    SHA256("SHA256"),   //
    SHA512("SHA512");   //
    private final String digestDesc;

    DigestType(String digestDesc) {
        this.digestDesc = digestDesc;
    }

    public static DigestType formString(String digestString) {
        for (DigestType digestType : DigestType.values()) {
            if (digestType.name().equalsIgnoreCase(digestString)) {
                return digestType;
            }
        }
        return null;
    }

    public String getDigestDesc() {
        return digestDesc;
    }
}
