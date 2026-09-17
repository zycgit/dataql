/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.dataql.udfs;

import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.FragmentProcess;

public class SqlFragmentUdf implements FragmentProcess {
    private final int index;

    public SqlFragmentUdf(int index) {
        this.index = index;
    }

    @Override
    public Object runFragment(Hints hint, Map<String, Object> params, String fragmentString) throws Throwable {
        return new HashMap<String, Object>() {{
            put("id", "id_" + index);
            put("name", "name_" + index);
            put("code", "code_" + index);
            put("body", fragmentString.trim());
            put("params", params);
        }};
    }
}
