/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function.encryt;
import com.alibaba.fastjson.JSON;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * Json函数。函数库引入 <code>import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;</code>
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
public class JsonUdfSource extends AbstractUdfSource {
    /** 把对象 JSON 序列化 */
    public String toJson(Object data) {
        return JSON.toJSONString(data);
    }

    /** 把对象 JSON 序列化（带格式） */
    public String toFmtJson(Object data) {
        return JSON.toJSONString(data, true);
    }

    /** 解析 JSON */
    public Object fromJson(String data) {
        if (data == null) {
            return null;
        }
        return JSON.parse(data);
    }
}
