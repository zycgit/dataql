/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.domain.Hints;

/**
 * 执行外部代码片段
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-03-28
 */
public interface FragmentProcess {
    /** 批量执行 */
    default List<Object> batchRunFragment(Hints hint, List<Map<String, Object>> params, String fragmentString) throws Throwable {
        List<Object> resultList = new ArrayList<>(params.size());
        for (Map<String, Object> paramItem : params) {
            resultList.add(this.runFragment(hint, paramItem, fragmentString));
        }
        return resultList;
    }

    /** 常规执行 */
    Object runFragment(Hints hint, Map<String, Object> params, String fragmentString) throws Throwable;
}
