/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.kernel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.*;
import net.hasor.dataql.host.Query;
import org.junit.Test;

public class FunRuntimeTest extends AbstractTestResource implements HintValue {
    private final Map<String, Object> object_list_map = new HashMap<String, Object>() {{
        put("list", new ArrayList<Object>() {{
            add("1");
            add("2");
            add(DomainHelper.convertTo("3"));
            add("4");
        }});
    }};

    @Test
    public void foo_1_Test() throws Exception {
        Map<String, Object> objectMap1 = new HashMap<String, Object>() {{
            put("udf", (Udf) (readOnly, values) -> object_list_map);
        }};
        //
        Query compilerQL = compilerQL("return ${udf}().list[0];");
        DataModel dataModel = compilerQL.execute(objectMap1).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isString();
        assert ((ValueModel) dataModel).asString().equals("1");
    }

    @Test
    public void foo_2_Test() throws Exception {
        //
        Udf udf = (readOnly, params) -> params.allParams();
        Query compilerQL = compilerQL("return ${_0}(1,2,3,4)[2];");
        DataModel dataModel = compilerQL.execute(new Object[] { udf }).getData();
        //
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).isNumber();
        assert ((ValueModel) dataModel).asInt() == 3;
    }
}
