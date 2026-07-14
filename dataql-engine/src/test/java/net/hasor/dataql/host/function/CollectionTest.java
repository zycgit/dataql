package net.hasor.dataql.host.function;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.ListModel;
import net.hasor.dataql.domain.ObjectModel;
import net.hasor.dataql.domain.ValueModel;
import net.hasor.dataql.host.function.basic.CollectionUdfSource;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.test.dataql.udfs.UserOrderUdfSource;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class CollectionTest {
    @Test
    public void merge() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "return collect.merge(0,[1,2],[3,4],5,6,[7,8],[9])";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.isList();
        assert ((ListModel) dataModel).size() == 10;
        assert ((ListModel) dataModel).getValue(5).asInt() == 5;
    }

    @Test
    public void filter() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "var dat = [0,1,2,3,4,5,6,7,8,9]; return collect.filter(dat,(obj) -> { return (obj >5) ? true : false })";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.isList();
        assert ((ListModel) dataModel).size() == 4;
        assert ((ListModel) dataModel).getValue(0).asInt() == 6;
        assert ((ListModel) dataModel).getValue(1).asInt() == 7;
        assert ((ListModel) dataModel).getValue(2).asInt() == 8;
        assert ((ListModel) dataModel).getValue(3).asInt() == 9;
    }

    @Test
    public void limit() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "var dat = [0,1,2,3,4,5,6,7,8,9]; return collect.limit(dat,3,3)";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        DataModel dataModel = dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.isList();
        assert ((ListModel) dataModel).size() == 3;
        assert ((ListModel) dataModel).getValue(0).asInt() == 3;
        assert ((ListModel) dataModel).getValue(1).asInt() == 4;
        assert ((ListModel) dataModel).getValue(2).asInt() == 5;
    }

    @Test
    public void list2map() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "import '" + UserOrderUdfSource.class.getName() + "' as data;";
        qlString = qlString + "return collect.list2map(data.userList(),'userID')";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        ObjectModel dataModel = (ObjectModel) dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.size() == 4;
        Set<String> strings = dataModel.asOri().keySet();
        assert strings.contains(String.valueOf(1));
        assert strings.contains(String.valueOf(2));
        assert strings.contains(String.valueOf(3));
        assert strings.contains(String.valueOf(4));
    }

    @Test
    public void map2list() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "import '" + UserOrderUdfSource.class.getName() + "' as data;";
        qlString = qlString + "return collect.map2list(data.userList()[0])";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        ListModel dataModel = (ListModel) dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.size() == 8;
        List<Object> unwrap = dataModel.unwrap();
        //
        assert unwrap.size() == 8;
    }

    @Test
    public void empty() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "return collect.isEmpty([])";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        ValueModel dataModel = (ValueModel) dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.asBoolean();
    }

    @Test
    public void empty2() throws IOException, QueryRuntimeException {
        String qlString = "";
        qlString = qlString + "import '" + CollectionUdfSource.class.getName() + "' as collect;";
        qlString = qlString + "if (collect.isEmpty([])) return true else return false;";
        //
        QueryBuilder dataQL = new QueryManager(new HostConfiguration().getHostContext()).newBuilder();
        ValueModel dataModel = (ValueModel) dataQL.createQuery(qlString).execute().getData();
        //
        assert dataModel.asBoolean();
    }
}
