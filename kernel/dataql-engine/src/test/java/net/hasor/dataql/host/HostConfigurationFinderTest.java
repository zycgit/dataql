package net.hasor.dataql.host;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.Udf;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.Finder;
import net.hasor.test.dataql.udfs.DataBean;
import net.hasor.test.dataql.udfs.DemoUdf;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HostConfigurationFinderTest extends AbstractTestResource {
    @Test
    public void registryIsFinder() {
        Finder finder = new HostConfiguration().getHostContext();
        assertNotNull(finder);
        assertTrue(finder instanceof HostContext);
    }

    @Test
    public void importProviderExecutesDataql() throws Exception {
        AtomicInteger invokeCount = new AtomicInteger();
        HostConfiguration finder = new HostConfiguration();
        finder.addImport(DemoUdf.class.getName(), () -> (Udf) (hints, params) -> {
            invokeCount.incrementAndGet();
            DataBean bean = new DataBean();
            bean.setName("Registry");
            return bean;
        });

        Query query = compilerQL("import '" + DemoUdf.class.getName() + "' as foo; return foo().name", finder);
        DataModel dataModel = query.execute().getData();

        assertTrue(dataModel.isValue());
        assertEquals("Registry", ((ValueModel) dataModel).asString());
        assertEquals(1, invokeCount.get());
    }

}
