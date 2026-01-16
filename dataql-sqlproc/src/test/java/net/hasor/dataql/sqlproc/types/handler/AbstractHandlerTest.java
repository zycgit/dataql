package net.hasor.dataql.sqlproc.types.handler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import net.hasor.dataql.sqlproc.utils.DsUtils;
import org.junit.After;
import org.junit.Before;

public class AbstractHandlerTest {
    protected Connection conn;

    @Before
    public void setup() throws SQLException {
        this.conn = DsUtils.h2Conn();
    }

    @After
    public void tearDown() throws SQLException {
        if (this.conn != null) {
            this.conn.close();
        }
    }

    @SuppressWarnings("unchecked")
    protected CallableStatement mockCallableStatement(Map<String, Object> returnValues) {
        return (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { CallableStatement.class }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.startsWith("get") && args != null && args.length > 0) {
                    // Find matching return value
                    Object val = returnValues.get(name);
                    if (val != null)
                        return val;

                    // fallback for generic getObject
                    if ("getObject".equals(name)) {
                        return returnValues.get("default");
                    }
                }
                if ("wasNull".equals(name)) {
                    return returnValues.get("wasNull") != null ? (Boolean) returnValues.get("wasNull") : false;
                }
                return null;
            }
        });
    }

    protected java.sql.PreparedStatement mockPreparedStatement(final Map<Integer, Object> captured) {
        return (java.sql.PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { java.sql.PreparedStatement.class }, (proxy, method, args) -> {
            if (method.getName().startsWith("set") && args.length >= 2) {
                captured.put((Integer) args[0], args[1]);
            }
            return null;
        });
    }

    protected java.sql.ResultSet mockResultSet(final Map<String, Object> values) {
        return (java.sql.ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { java.sql.ResultSet.class }, (proxy, method, args) -> {
            String name = method.getName();
            if (name.startsWith("get")) {
                if (values.containsKey(name)) {
                    return values.get(name);
                }
                if (values.containsKey("default")) {
                    return values.get("default");
                }
            }
            if ("wasNull".equals(name)) {
                return values.getOrDefault("wasNull", false);
            }
            return null;
        });
    }
}
