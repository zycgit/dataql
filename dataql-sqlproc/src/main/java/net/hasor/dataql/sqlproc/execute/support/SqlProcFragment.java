package net.hasor.dataql.sqlproc.execute.support;

import java.util.List;
import java.util.Map;
import net.hasor.dataql.FragmentProcess;
import net.hasor.dataql.Hints;

public class SqlProcFragment implements FragmentProcess {
    // RootStatement
    // FxSqlCheckChainSpi
    // LookupConnectionListener
    // LookupDataSourceListener

    @Override
    public List<Object> batchRunFragment(Hints hint, List<Map<String, Object>> params, String fragmentString) throws Throwable {
        return null;
    }

    @Override
    public Object runFragment(Hints hint, Map<String, Object> params, String fragmentString) throws Throwable {
        return null;
    }
}
