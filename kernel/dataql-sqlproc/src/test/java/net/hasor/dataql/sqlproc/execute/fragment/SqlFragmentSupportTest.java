/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
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
