/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于封装 Hint。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class HintsSet implements Hints {
    private final Map<String, Object> optionMap;

    public HintsSet() {
        this.optionMap = new HashMap<>();
    }

    public HintsSet(Hints optionSet) {
        this.optionMap = new HashMap<>();
        optionSet.forEach(this.optionMap::put);
    }

    @Override
    public String[] getHints() {
        return this.optionMap.keySet().toArray(new String[0]);
    }

    /** 获取选项参数 */
    public Object getHint(String optionKey) {
        return this.optionMap.get(optionKey);
    }

    /** 删除选项参数 */
    public void removeHint(String key) {
        this.optionMap.remove(key);
    }

    @Override
    public void setHint(String hintName, Object value) {
        this.optionMap.put(hintName, value);
    }
}
