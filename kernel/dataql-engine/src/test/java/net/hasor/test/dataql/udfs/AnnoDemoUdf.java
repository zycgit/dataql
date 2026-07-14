package net.hasor.test.dataql.udfs;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;

//@DimUdf("test")
public class AnnoDemoUdf implements Udf {
    @Override
    public Object call(Hints readOnly, Object[] params) {
        return "test";
    }
}