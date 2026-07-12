package net.hasor.dataql.sqlproc.execute.fragment;

import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class SqlFragmentSupportTest extends AbstractSqlProcTest {
    @Test
    public void newQueryContextUsesSqlQueryContext() {
        assertTrue(newQueryContext() instanceof ExecuteContext);
    }

    @Test(expected = NullPointerException.class)
    public void nullQueryContextFails() {
        new SelectFragmentProcess(null);
    }
}
