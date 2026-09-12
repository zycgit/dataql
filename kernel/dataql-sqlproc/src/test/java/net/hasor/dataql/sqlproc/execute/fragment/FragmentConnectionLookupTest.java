/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute.fragment;

import java.sql.DriverManager;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.sqlproc.AbstractSqlProcTest;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.execute.support.ExecuteContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FragmentConnectionLookupTest extends AbstractSqlProcTest {
    @Test
    public void fragmentFindsConnectionThroughQueryContext() throws Throwable {
        AtomicReference<String> sourceName = new AtomicReference<>();
        AtomicReference<Hints> observedHints = new AtomicReference<>();
        ExecuteContext queryContext = newQueryContext((name, hints) -> {
            sourceName.set(name);
            observedHints.set(hints);
            return DriverManager.getConnection("jdbc:h2:mem:query_context_connection", "sa", "");
        });
        SelectFragmentProcess fragment = new SelectFragmentProcess(queryContext);
        HintsSet hints = new HintsSet();
        hints.setHint(SqlHintNames.FRAGMENT_SQL_DATA_SOURCE.name(), "reporting");
        hints.setHint("tenant", "north");

        Object result = fragment.runFragment(hints, Collections.emptyMap(), "SELECT 1");

        assertEquals(1, ((Number) result).intValue());
        assertEquals("reporting", sourceName.get());
        assertSame(hints, observedHints.get());
        assertEquals("north", observedHints.get().getHint("tenant"));
    }
}
