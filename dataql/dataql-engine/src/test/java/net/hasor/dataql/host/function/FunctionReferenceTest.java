/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FunctionReferenceTest {
    private Object execute(String library, String body) throws Exception {
        String script = "import 'net.hasor.dataql.host.function." + library + "' as fn;\n" + body;
        return new QueryManager(new HostConfiguration()).newBuilder().createQuery(script).execute().getData().unwrap();
    }

    @Test
    public void stringSearchAndEmptyInputFollowTheirContracts() throws Exception {
        assertEquals(3, this.execute("basic.StringUdfSource", "return fn.lastIndexOfIgnoreCase('AbCaBc', 'aB');"));
        assertEquals("", this.execute("basic.StringUdfSource", "return fn.humpToLine('');"));
        assertNull(this.execute("basic.StringUdfSource", "return fn.split(null, ',');"));
        assertEquals(-1, this.execute("basic.StringUdfSource", "return fn.indexOfWithStart('abc', null, 0);"));
        assertEquals(-1, this.execute("basic.StringUdfSource", "return fn.lastIndexOf('abc', null);"));
        assertEquals(-1, this.execute("basic.StringUdfSource", "return fn.lastIndexOfWithStart('abc', null, 2);"));
    }

    @Test
    public void codecAcceptsTheNumericListsProducedByScripts() throws Exception {
        assertEquals("QUI=", this.execute("encryt.CodecUdfSource", "return fn.encodeBytes([65,66]);"));
        assertEquals(this.execute("encryt.CodecUdfSource", "return fn.digestString('SHA256', 'AB');"), this.execute("encryt.CodecUdfSource", "return fn.digestBytes('SHA256', [65,66]);"));
        assertEquals(this.execute("encryt.CodecUdfSource", "return fn.hmacString('HmacSHA256', 'key', 'AB');"), this.execute("encryt.CodecUdfSource", "return fn.hmacBytes('HmacSHA256', 'key', [65,66]);"));
    }

    @Test
    public void collectionCallbacksCanExitTheWholeQuery() throws Exception {
        assertEquals("done", this.execute("basic.CollectionUdfSource", "run fn.list2map([1], (i,v) -> { exit 'done'; }); return 'unreachable';"));
    }

    @Test
    public void groupingAndDeduplicationUseFieldNames() throws Exception {
        String data = "var rows = [{'id':1,'tag':'a'},{'id':2,'tag':'a'},{'id':3,'tag':'b'}]; ";
        assertEquals(Map.of("a", List.of(Map.of("id", (byte) 1, "tag", "a"), Map.of("id", (byte) 2, "tag", "a")), "b", List.of(Map.of("id", (byte) 3, "tag", "b"))), this.execute("basic.CollectionUdfSource", data + "return fn.groupBy(rows, 'tag');"));
        assertEquals(List.of(Map.of("id", (byte) 1, "tag", "a"), Map.of("id", (byte) 3, "tag", "b")), this.execute("basic.CollectionUdfSource", data + "return fn.uniqueBy(rows, 'tag');"));
        assertEquals(Map.of(), this.execute("basic.CollectionUdfSource", "return fn.list2map(null, 'id');"));
    }

    @Test
    public void sortingAcceptsSmallAndWideIntegerComparatorResults() throws Exception {
        for (String width : new String[] { "byte", "long", "big" }) {
            String script = "hint MIN_INTEGER_WIDTH = '" + width + "'; " + "import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect; " + "return collect.listSort([3,1,2], (a,b) -> { return a < b ? -1 : (a == b ? 0 : 1); });";
            Object result = new QueryManager(new HostConfiguration()).newBuilder().createQuery(script).execute().getData().unwrap();
            List<?> sorted = (List<?>) result;
            for (int index = 0; index < sorted.size(); index++) {
                assertEquals(index + 1, ((Number) sorted.get(index)).intValue());
            }
        }
    }
}
