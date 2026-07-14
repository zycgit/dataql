/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.function.Function;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.host.spi.FragmentProcessFactory;
import net.hasor.dataql.kernel.FragmentProcess;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;

abstract class AbstractSqlFragmentProcessFactory implements FragmentProcessFactory {
    private final String[]                                  names;
    private final Function<ExecuteContext, FragmentProcess> creator;

    protected AbstractSqlFragmentProcessFactory(String operation, Function<ExecuteContext, FragmentProcess> creator) {
        this.names = new String[] { operation + "Sql", operation + "Xml" };
        this.creator = creator;
    }

    @Override
    public String[] getNames() {
        return this.names.clone();
    }

    @Override
    public FragmentProcess create(String name, HostContext context) {
        return this.creator.apply(context.getAttachment(ExecuteContext.class));
    }
}
