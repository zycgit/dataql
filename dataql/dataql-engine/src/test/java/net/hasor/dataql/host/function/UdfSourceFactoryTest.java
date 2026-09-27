/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host.function;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.util.JsonUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class UdfSourceFactoryTest {
    @Test
    public void dataqlContextDiscoversFunctionAliasesBySpi() throws Exception {
        Query query = new QueryManager(new HostConfiguration().getHostContext()).newBuilder().createQuery("""
                import 'net.hasor.dataql.host.function.basic.CollectionUdfSource' as collect;
                import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
                import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
                return string.toLowerCase("ABC") + ":" + collect.size([1, 2, 3]) + ":" + json.toJson([1, 2]);
                """);

        ValueModel data = (ValueModel) query.execute().getData();

        assertTrue(data.isValue());
        assertEquals("abc:3:[1,2]", data.asString());
    }

    @Test
    public void jsonFunctionsRoundTripNestedDataWithoutLosingDecimalPrecision() throws Exception {
        Query query = new QueryManager(new HostConfiguration().getHostContext()).newBuilder().createQuery("""
                import 'net.hasor.dataql.host.function.encryt.JsonUdfSource' as json;
                var value = json.fromJson('{"text":"雪","values":[1,true,null],"empty":null,"decimal":1234567890.123456789}');
                return {
                    'compact': json.toJson(value),
                    'formatted': json.toFmtJson(value),
                    'restored': json.fromJson(json.toFmtJson(value)),
                    'nullValue': json.fromJson(null),
                    'emptyValue': json.fromJson(' ')
                };
                """);
        Map<?, ?> result = (Map<?, ?>) query.execute().getData().unwrap();
        Map<?, ?> restored = (Map<?, ?>) result.get("restored");
        assertEquals("雪", restored.get("text"));
        assertTrue(restored.containsKey("empty"));
        assertNull(restored.get("empty"));
        List<?> values = (List<?>) restored.get("values");
        assertEquals(1, ((Number) values.get(0)).intValue());
        assertEquals(Boolean.TRUE, values.get(1));
        assertNull(values.get(2));
        assertEquals(new BigDecimal("1234567890.123456789"), restored.get("decimal"));
        assertEquals(restored, JsonUtils.readValue((String) result.get("compact"), Map.class));
        assertEquals(restored, JsonUtils.readValue((String) result.get("formatted"), Map.class));
        assertTrue(((String) result.get("formatted")).contains("\n"));
        assertNull(result.get("nullValue"));
        assertNull(result.get("emptyValue"));
    }
}
