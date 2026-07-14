package net.hasor.test.dataql.udfs;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;

public class DemoUdf implements Udf {
    @Override
    public Object call(Hints readOnly, Object[] params) {
        return new DataBean();
    }
}