package net.hasor.dataql.host.function;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.function.basic.StateUdfSource;
import net.hasor.dataql.kernel.QueryRuntimeException;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

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
