/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.types.handler;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.hasor.dataql.sqlproc.types.bytes.BlobAsBytesTypeHandler;
import net.hasor.dataql.sqlproc.types.bytes.BlobAsBytesWrapTypeHandler;
import net.hasor.dataql.sqlproc.types.string.ClobAsStringTypeHandler;
import net.hasor.dataql.sqlproc.types.string.NClobAsStringTypeHandler;
import org.junit.Test;

public class BlobClobTypeHandlerTest extends TypeHandlerMockSupport {

    private Clob mockClob(String content) {
        return (Clob) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Clob.class }, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return (long) content.length();
            }
            if ("getSubString".equals(method.getName())) {
                return content;
            }
            return null;
        });
    }

    private NClob mockNClob(String content) {
        return (NClob) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { NClob.class }, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return (long) content.length();
            }
            if ("getSubString".equals(method.getName())) {
                return content;
            }
            return null;
        });
    }

    private Blob mockBlob(byte[] content) {
        return (Blob) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Blob.class }, (proxy, method, args) -> {
            if ("length".equals(method.getName())) {
                return (long) content.length;
            }
            if ("getBytes".equals(method.getName())) {
                return content;
            }
            return null;
        });
    }

    @Test
    public void testClobAsStringTypeHandler_CallableStatement() throws Throwable {
        ClobAsStringTypeHandler handler = new ClobAsStringTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String val = "clob-test";

        values.put("getClob", mockClob(val));

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testNClobAsStringTypeHandler_CallableStatement() throws Throwable {
        NClobAsStringTypeHandler handler = new NClobAsStringTypeHandler();
        Map<String, Object> values = new HashMap<>();
        String val = "nclob-test";

        values.put("getNClob", mockNClob(val));

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert val.equals(result);
    }

    @Test
    public void testBlobAsBytesTypeHandler_CallableStatement() throws Throwable {
        BlobAsBytesTypeHandler handler = new BlobAsBytesTypeHandler();
        Map<String, Object> values = new HashMap<>();
        byte[] val = new byte[] { 1, 2, 3 };

        values.put("getBlob", mockBlob(val));

        CallableStatement cs = mockCallableStatement(values);
        Object result = handler.getResult(cs, 1);
        assert Arrays.equals(val, (byte[]) result);
    }

    @Test
    public void testBlobAsBytesWrapTypeHandler_CallableStatement() throws Throwable {
        BlobAsBytesWrapTypeHandler handler = new BlobAsBytesWrapTypeHandler();
        Map<String, Object> values = new HashMap<>();
        byte[] val = new byte[] { 1, 2, 3 }; // Blob returns primitive byte[]

        values.put("getBlob", mockBlob(val));

        CallableStatement cs = mockCallableStatement(values);
        Byte[] result = (Byte[]) handler.getResult(cs, 1);
        assert result.length == 3;
        assert result[0] == 1;
        assert result[1] == 2;
        assert result[2] == 3;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testClob() throws Throwable {
        String val = "clob-test-string";
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_char_lage) values (?)")) {
            // ClobAsStringTypeHandler uses ps.setClob or setCharacterStream usually
            // but setParameter logic in ClobAsStringTypeHandler simply uses setString or setClob
            // Let's check implementation if test fails.
            new ClobAsStringTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_char_lage from tb_h2_types limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new ClobAsStringTypeHandler().getResult(rs, 1);
                assert val.equals(res);
            }
        }
    }

    @Test
    public void testBlob() throws Throwable {
        byte[] val = new byte[] { 10, 20, 30 };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_binary_lage) values (?)")) {
            new BlobAsBytesTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_binary_lage from tb_h2_types limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Object res = new BlobAsBytesTypeHandler().getResult(rs, 1);
                assert res instanceof byte[];
                assert Arrays.equals(val, (byte[]) res);
            }
        }
    }

    @Test
    public void testBlobAsBytesWrap() throws Throwable {
        Byte[] val = new Byte[] { 10, 20, 30 };
        try (PreparedStatement ps = conn.prepareStatement("insert into tb_h2_types (c_binary_lage) values (?)")) {
            new BlobAsBytesWrapTypeHandler().setParameter(ps, 1, val, null);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("select c_binary_lage from tb_h2_types limit 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Byte[] res = (Byte[]) new BlobAsBytesWrapTypeHandler().getResult(rs, 1);
                assert Arrays.equals(val, res);
            }
        }
    }
}
