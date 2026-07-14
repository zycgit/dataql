package net.hasor.dataql.kernel;
import net.hasor.dataql.ConfigOption;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.AbstractTestResource;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.compiler.CompilerArguments;
import net.hasor.test.dataql.udfs.ErrorUdf;
import org.junit.Test;

public class ErrorTest extends AbstractTestResource {
    @Test
    public void udf_error() throws QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + ErrorUdf.class.getName() + "' as err;\n";
        qlString = qlString + "return err(a)";
        //
        try {
            QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
            DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
            assert false;
        } catch (Exception e) {
            e.printStackTrace();
            assert e instanceof RuntimeException;
            assert e.getCause() == ErrorUdf.ERR;
        }
    }

    @Test
    public void lambda_error() throws Throwable {
        String qlString = "";
        qlString = qlString + "var err = () -> throw 123, 'abc';\n";
        qlString = qlString + "var abc = err(); return 12345";
        //
        try {
            QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
            dataQL.configOption(ConfigOption.CODE_LOCATION, CompilerArguments.CodeLocationEnum.TERM);
            DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
            assert false;
        } catch (ThrowRuntimeException e) {
            assert e.getLocation().toString().equalsIgnoreCase("line 1:16~1:32 ,QIL 1:4");
            assert e.getThrowCode() == 123;
            assert e.getResult().isValue();
            assert ((ValueModel) e.getResult()).asString().equals("abc");
        }
    }

    @Test
    public void eval_error() throws Throwable {
        String qlString = "";
        qlString = qlString + "var dat1 = 1;\n";
        qlString = qlString + "return null / dat1";
        //
        try {
            QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
            dataQL.configOption(ConfigOption.CODE_LOCATION, CompilerArguments.CodeLocationEnum.TERM);
            DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
            assert false;
        } catch (QueryRuntimeException e) {
            assert e.getLocation().toString().equalsIgnoreCase("line 2:12~2:13 ,QIL 0:9");
            assert e.getMessage().endsWith(" DO -> first data is null.");
        }
    }
}
