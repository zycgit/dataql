/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.domain;
import java.math.BigDecimal;
import java.util.Arrays;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class HintReferenceTest {
    private Query query(String script) throws Exception {
        return new QueryManager(new HostConfiguration()).newBuilder().createQuery(script);
    }

    @Test
    public void negativeFirstIndexIsInBoundsForEveryPolicy() throws Exception {
        for (String policy : new String[] { "near", "null", "throw" }) {
            assertEquals((byte) 1, this.query("hint INDEX_OVERFLOW = '" + policy + "'; var values = [1,2]; return values[-2];").execute().getData().unwrap());
        }
    }

    @Test
    public void emptyListHasNoNearestElement() throws Exception {
        for (String policy : new String[] { "near", "null" }) {
            assertEquals(Arrays.asList(null, null), this.query("hint INDEX_OVERFLOW = '" + policy + "'; var values = []; return [values[0],values[-1]];").execute().getData().unwrap());
        }
        assertThrows(Exception.class, () -> this.query("hint INDEX_OVERFLOW = 'throw'; var values = []; return values[0];").execute());
    }

    @Test
    public void roundingAndScopedOverridesMatchTheReference() throws Exception {
        Query query = this.query("""
                hint MIN_DECIMAL_WIDTH = 'big';
                hint MAX_DECIMAL_DIGITS = 2;
                hint NUMBER_ROUNDING = 'DOWN';
                var inner = () -> { hint NUMBER_ROUNDING = 'UP'; return 1 / 6; };
                return [inner(), 1 / 6];
                """);
        assertEquals(Arrays.asList(new BigDecimal("0.17"), new BigDecimal("0.16")), query.execute().getData().unwrap());
    }

    @Test
    public void cloningPreservesHintsAndObjectReferences() throws Exception {
        Query original = this.query("return 1;");
        Object attachment = new Object();
        original.setHint("attachment", attachment);
        original.setHint("MIN_INTEGER_WIDTH", "long");
        Query copy = original.clone();
        assertSame(attachment, copy.getHint("attachment"));
        assertEquals(1L, copy.execute().getData().unwrap());
        copy.removeHint("attachment");
        assertSame(attachment, original.getHint("attachment"));
    }

    @Test
    public void declaredWidthsMatchTheActualUnconfiguredLowerBounds() throws Exception {
        assertEquals("byte", HintNames.MIN_INTEGER_WIDTH.getDefaultVal());
        assertEquals("float", HintNames.MIN_DECIMAL_WIDTH.getDefaultVal());
        assertEquals(Byte.class, this.query("return 1;").execute().getData().unwrap().getClass());
        assertEquals(Float.class, this.query("return 0.5;").execute().getData().unwrap().getClass());
    }

    @Test
    public void nestedReturnRestoresHintsAndFailedCallbacksDoNotLeakThem() throws Exception {
        Query query = this.query("""
                hint INDEX_OVERFLOW = 'near';
                import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                var values = [1,2];
                var inner = () -> {
                    hint INDEX_OVERFLOW = 'null';
                    if (true) { hint MIN_INTEGER_WIDTH = 'long'; return values[9]; }
                };
                var first = inner();
                run collect.list2map([1], (i,v) -> { hint INDEX_OVERFLOW = 'null'; throw 'failed'; });
                return [first, values[9]];
                """);
        assertEquals(Arrays.asList(null, (byte) 2), query.execute().getData().unwrap());
    }
}