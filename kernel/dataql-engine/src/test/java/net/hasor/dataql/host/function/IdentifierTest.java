/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;
import java.io.IOException;
import java.util.UUID;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.host.function.basic.StateUdfSource;
import net.hasor.dataql.kernel.QueryRuntimeException;
import org.junit.Test;

public class IdentifierTest {
    @Test
    public void uuid() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + StateUdfSource.class.getName() + "' as state;";
        qlString = qlString + "return state.uuid()";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        String uuid = ((ValueModel) dataModel).asString();
        assert uuid.length() == UUID.randomUUID().toString().length();
    }

    @Test
    public void uuid2() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + StateUdfSource.class.getName() + "' as state;";
        qlString = qlString + "return state.uuidToShort();";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        String uuid = ((ValueModel) dataModel).asString();
        assert uuid.length() == UUID.randomUUID().toString().replace("-", "").length();
    }
}
