package net.hasor.test.dataql.udfs;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.Udf;

public class ErrorUdf implements Udf {
    public static RuntimeException ERR = new RuntimeException("abc");

    @Override
    public Object call(Hints readOnly, Object[] params) {
        throw ERR;
    }
}