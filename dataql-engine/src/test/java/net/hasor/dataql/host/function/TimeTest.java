package net.hasor.dataql.host.function;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.kernel.QueryRuntimeException;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeTest {
    @Test
    public void now() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;";
        qlString = qlString + "return time.now()";
        //
        long t1 = System.currentTimeMillis();
        QueryManager dataQL = new QueryManager();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        long t2 = ((ValueModel) dataModel).asLong();
        long t3 = System.currentTimeMillis();
        assert t1 <= t2 && t2 <= t3;
    }

    @Test
    public void time() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;";
        qlString = qlString + "var now = time.now() ; return (time.year(now) + '-' + time.month(now) + '-' + time.day(now))";
        //
        QueryManager dataQL = new QueryManager();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        String format = new SimpleDateFormat("yyyy-M-d").format(new Date());
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asString().equalsIgnoreCase(format);
    }

    @Test
    public void format() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;";
        qlString = qlString + "return time.format(time.now(),'yyyy-MM-dd')";
        //
        QueryManager dataQL = new QueryManager();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        String format = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asString().equalsIgnoreCase(format);
    }

    @Test
    public void parse() throws IOException, QueryRuntimeException, ParseException {
        String qlString = "";
        qlString = qlString + "import 'net.hasor.dataql.host.function.basic.DateTimeUdfSource' as time;";
        qlString = qlString + "return time.parser('2017-03-15 12:44:56','yyyy-MM-dd hh:mm:ss')";
        //
        QueryManager dataQL = new QueryManager();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        long format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2017-03-15 12:44:56").getTime();
        assert dataModel.isValue();
        assert ((ValueModel) dataModel).asLong() == format;
    }
}
