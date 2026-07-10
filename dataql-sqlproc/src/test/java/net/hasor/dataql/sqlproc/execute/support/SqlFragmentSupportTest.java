package net.hasor.dataql.sqlproc.execute.support;

import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.execute.SqlQueryContext;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class SqlFragmentSupportTest extends AbstractSqlProcTest {
    @Test
    public void newQueryContextUsesSqlQueryContext() {
        assertTrue(newQueryContext() instanceof SqlQueryContext);
    }

    @Test(expected = NullPointerException.class)
    public void nullConnectionSupplierFails() {
        new SelectFragmentProcess(null, newQueryContext());
    }

    @Test(expected = NullPointerException.class)
    public void nullQueryContextFails() {
        new SelectFragmentProcess(name -> newH2WithUsers(), null);
    }
}
