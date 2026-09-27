/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel.mem;
import java.util.*;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.location.RuntimeLocation;

/**
 * 代理 Fragment 使其成为 UDF.
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public class RefFragmentCall implements Udf {
    private final RuntimeLocation location;
    private final boolean         isBach;
    private final String          fragmentType;
    private final FragmentProcess fragmentProcess;

    public RefFragmentCall(RuntimeLocation location, boolean isBach, String fragmentType, FragmentProcess fragmentProcess) {
        this.location = location;
        this.isBach = isBach;
        this.fragmentType = fragmentType;
        this.fragmentProcess = fragmentProcess;
    }

    @Override
    public Object call(Hints readOnly, UdfParams params) throws Throwable {
        Object[] values = params.allParams();
        String fragmentString = values[1].toString();
        Map<String, Object> fragmentParams = (Map<String, Object>) values[0];
        Hints fragmentHints = new HintsProxy(readOnly) {
            @Override
            public Object getHint(String optionKey) {
                if (HintNames.FRAGMENT_TYPE.name().equals(optionKey)) {
                    return fragmentType;
                }
                return super.getHint(optionKey);
            }
        };

        if (this.isBach) {
            List<Map<String, Object>> fragmentParamsArray = new ArrayList<>();
            Map<String, Integer> argsLengthMap = new TreeMap<>();
            int lastSize = -1;
            boolean argsLengthError = false;

            for (String key : fragmentParams.keySet()) {
                // .参数类型校验
                Object dataModel = fragmentParams.get(key);
                if (!(dataModel instanceof List<?> listData)) {
                    throw new QueryRuntimeException(this.location, "The batch fragment args must be an array.");
                }

                // .参数长度校验
                int tmpSize = listData.size();
                argsLengthMap.put(key, tmpSize);
                if (lastSize < 0) {
                    lastSize = tmpSize;
                } else {
                    if (tmpSize != lastSize) {
                        argsLengthError = true;
                    }
                }

                // .参数拆分
                //      param1,    param2        => [ {param1:1,param2:1}, {param1:2,param2:2}, {param1:3,param2:3} ]
                //        [1,2,3]    [1,2,3]
                if (!argsLengthError) {
                    for (int i = 0; i < listData.size(); i++) {
                        if (i >= fragmentParamsArray.size()) {
                            fragmentParamsArray.add(new HashMap<>());
                        }
                        Map<String, Object> objectMap = fragmentParamsArray.get(i);
                        objectMap.put(key, listData.get(i));
                    }
                }
            }

            if (argsLengthError) {
                StringBuilder strBuild = new StringBuilder();
                argsLengthMap.forEach((key, integer) -> {
                    strBuild.append(key + "=" + integer + ",");
                });
                if (!strBuild.isEmpty()) {
                    strBuild.deleteCharAt(strBuild.length() - 1);
                }
                throw new QueryRuntimeException(this.location, "batch fragment,All args must have the same length -> [" + strBuild + "]");
            }

            return this.fragmentProcess.batchRunFragment(fragmentHints, fragmentParamsArray, fragmentString);
        } else {
            return this.fragmentProcess.runFragment(fragmentHints, fragmentParams, fragmentString);
        }
    }
}
