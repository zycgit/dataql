package net.hasor.dataql.host;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HostConfigurationTest {
    @Test
    public void customResourceLoaderCompilesImport() throws Exception {
        ResourceLoader resourceLoader = new ClassPathResourceLoader() {
            @Override
            public InputStream getResourceAsStream(String resource) {
                if ("custom.ql".equals(resource)) {
                    return new ByteArrayInputStream("return 'CustomResource'".getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(resource);
            }
        };
        HostConfiguration parent = new HostConfiguration();
        parent.setResourceLoader(resourceLoader);
        HostConfiguration configuration = new HostConfiguration(parent);
        QueryManager queryManager = new QueryManager(configuration.getHostContext());

        Query query = queryManager.createQuery("import @\"custom.ql\" as custom; return custom()");
        DataModel dataModel = query.execute().getData();

        assertSame(resourceLoader, configuration.getResourceLoader());
        assertTrue(dataModel.isValue());
        assertEquals("CustomResource", ((ValueModel) dataModel).asString());
    }
}
