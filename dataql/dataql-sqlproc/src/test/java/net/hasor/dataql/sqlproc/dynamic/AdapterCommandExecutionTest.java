/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic;
import java.sql.Connection;
import java.sql.PreparedStatement;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/** Verifies the DataQL-to-JDBC path without running external database servers. */
public class AdapterCommandExecutionTest {
    @Test
    public void elasticQuerySeparatorsDoNotBindAsParameters() throws Exception {
        this.execute("PUT /users/_doc/1\\?refresh=true\\&pretty {\"id\":#{id},\"name\":#{name}}", "PUT /users/_doc/1?refresh=true&pretty {\"id\":?,\"name\":?}");
    }

    @Test
    public void mongoDocumentValuesStayBound() throws Exception {
        this.execute("db.users.insertOne({id:#{id},name:#{name}})", "db.users.insertOne({id:?,name:?})");
    }

    @Test
    public void redisKeyColonStaysLiteral() throws Exception {
        this.execute("HSET user\\:1 id #{id} name #{name}", "HSET user:1 id ? name ?");
    }

    @Test
    public void milvusBm25OperatorStaysLiteral() throws Exception {
        this.execute("SELECT * FROM users WHERE id=#{id} ORDER BY text <\\?> #{name}", "SELECT * FROM users WHERE id=? ORDER BY text <?> ?");
    }

    private void execute(String command, String prepared) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(prepared)).thenReturn(statement);
        when(statement.getUpdateCount()).thenReturn(1);
        HostConfiguration host = new HostConfiguration();
        host.addAttachment(ConnectionProvider.class, (name, hints) -> connection);
        String script = "var call = @@updateSql(id,name)<%" + command + "%>; return call(7, 'x\\\" :fake ? #{fake}');";
        Object result = new QueryManager(host).newBuilder().createQuery(script).execute().getData().unwrap();
        assertEquals(1, ((Number) result).intValue());
        verify(connection).prepareStatement(prepared);
        verify(statement).setByte(1, (byte) 7);
        verify(statement).setString(2, "x\" :fake ? #{fake}");
        verify(statement, never()).setObject(anyInt(), any());
        verify(statement).execute();
        verify(statement).close();
    }
}
