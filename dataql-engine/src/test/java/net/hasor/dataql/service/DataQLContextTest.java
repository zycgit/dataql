package net.hasor.dataql.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.cobble.loader.providers.ClassPathResourceLoader;
import net.hasor.dataql.Finder;
import net.hasor.dataql.Query;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DataQLContextTest {
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
        DataQLContext context = new DataQLContext(Finder.DEFAULT, resourceLoader);

        Query query = context.createQuery("import @\"custom.ql\" as custom; return custom()");
        DataModel dataModel = query.execute().getData();

        assertSame(resourceLoader, context.getResourceLoader());
        assertTrue(dataModel.isValue());
        assertEquals("CustomResource", ((ValueModel) dataModel).asString());
    }
}
