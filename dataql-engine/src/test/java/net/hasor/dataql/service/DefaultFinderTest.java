package net.hasor.dataql.service;

import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.Finder;
import net.hasor.dataql.Query;
import net.hasor.dataql.Udf;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.test.dataql.udfs.DataBean;
import net.hasor.test.dataql.udfs.DemoUdf;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultFinderTest extends AbstractTestResource {
    @Test
    public void defaultFinderUsesDefaultImplementation() {
        assertTrue(Finder.DEFAULT instanceof DefaultFinder);
        assertTrue(new DataQLContext().getFinder() == Finder.DEFAULT);
    }

    @Test
    public void importProviderExecutesDataql() throws Exception {
        AtomicInteger invokeCount = new AtomicInteger();
        DefaultFinder finder = new DefaultFinder();
        finder.addImport(DemoUdf.class.getName(), () -> (Udf) (hints, params) -> {
            invokeCount.incrementAndGet();
            DataBean bean = new DataBean();
            bean.setName("DefaultFinder");
            return bean;
        });

        Query query = compilerQL("import '" + DemoUdf.class.getName() + "' as foo; return foo().name", finder);
        DataModel dataModel = query.execute().getData();

        assertTrue(dataModel.isValue());
        assertEquals("DefaultFinder", ((ValueModel) dataModel).asString());
        assertEquals(1, invokeCount.get());
    }

}
