package net.hasor.dataql.host.function;

import net.hasor.dataql.host.Query;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
