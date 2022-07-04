package net.hasor.dataql.sqlproc.dialect;
import java.util.Arrays;

/**
 * SQL
 * @version : 2020-10-31
 * @author 赵永春 (zyc@hasor.net)
 */
public class BoundSqlObject implements BoundSql {
    /** SQL */
    private final String   sqlString;
    private final Object[] paramArray;

    public BoundSqlObject(String sqlString) {
        this.sqlString = sqlString;
        this.paramArray = new Object[0];
    }

    public BoundSqlObject(String sqlString, Object[] paramArray) {
        this.sqlString = sqlString;
        this.paramArray = paramArray;
    }

    public String getSqlString() {
        return this.sqlString;
    }

    @Override
    public Object[] getArgs() {
        return this.paramArray;
    }

    @Override
    public String toString() {
        return "BoundSqlObj{'" + sqlString + '\'' + ", args=" + Arrays.toString(paramArray) + '}';
    }
}
