/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.spi;

import java.util.Map;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

public class TestFragmentProcessFactory implements FragmentProcessFactory {
    @Override
    public String[] getNames() {
        return new String[] { "testFragment", "testFragmentAlias" };
    }

    @Override
    public FragmentProcess create(String name, HostContext context) {
        return new FragmentProcess() {
            @Override
            public Object runFragment(Hints hints, Map<String, Object> params, String fragmentString) {
                return fragmentString;
            }
        };
    }
}
