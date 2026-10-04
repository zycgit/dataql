/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc;
import net.hasor.dataql.domain.HintsSet;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SqlHintDefaultsTest {
    @Test
    public void unnamedAliasesMustNotReplaceDefaults() {
        HintsSet hints = new HintsSet();
        assertEquals("column", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE));
        assertEquals("", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_DATA_SOURCE));
        assertEquals("0", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET));
        assertNull(SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE.getShortName());
        hints.setHint("column", "off");
        assertEquals("column", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_OPEN_PACKAGE));
    }

    @Test
    public void shortNamesOverrideLongNamesOnlyWhenDeclared() {
        HintsSet hints = new HintsSet();
        hints.setHint("FRAGMENT_SQL_TIMEOUT", 3);
        hints.setHint("timeout", 5);
        assertEquals("5", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_TIMEOUT));
        hints.removeHint("timeout");
        assertEquals("3", SqlHintNames.getValue(hints, SqlHintNames.FRAGMENT_SQL_TIMEOUT));
    }
}
