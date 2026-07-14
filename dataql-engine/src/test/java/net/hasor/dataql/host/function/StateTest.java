package net.hasor.dataql.host.function;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.host.function.basic.StateUdfSource;
import net.hasor.dataql.kernel.QueryRuntimeException;
import org.junit.Test;

import java.io.IOException;

public class StateTest {
    @Test
    public void decNumber() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + StateUdfSource.class.getName() + "' as state;";
        qlString = qlString + "var decNum = state.decNumber(0); return [ decNum(),decNum(),decNum() ]";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        assert ((ListModel) dataModel).getValue(0).asLong() == 1;
        assert ((ListModel) dataModel).getValue(1).asLong() == 2;
        assert ((ListModel) dataModel).getValue(2).asLong() == 3;
    }

    @Test
    public void incNumber() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + StateUdfSource.class.getName() + "' as state;";
        qlString = qlString + "var decNum = state.incNumber(0); return [ decNum(),decNum(),decNum() ]";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        assert ((ListModel) dataModel).getValue(0).asLong() == -1;
        assert ((ListModel) dataModel).getValue(1).asLong() == -2;
        assert ((ListModel) dataModel).getValue(2).asLong() == -3;
    }
}
