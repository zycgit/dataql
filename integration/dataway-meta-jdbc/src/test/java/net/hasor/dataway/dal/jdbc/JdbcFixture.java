/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.hasor.dataway.dal.FieldDef;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import static net.hasor.dataway.dal.FieldDef.*;

final class JdbcFixture implements AutoCloseable {
    final JdbcDataSource      source = new JdbcDataSource();
    final JdbcDataAccessLayer access;

    JdbcFixture() throws Exception {
        this.source.setURL("jdbc:h2:mem:metadata_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1");
        this.load("/META-INF/dataway/schema/h2.sql");
        this.access = new JdbcDataAccessLayer(this.source);
    }

    void load(String resource) throws Exception {
        try (Connection connection = this.source.getConnection(); var input = JdbcFixture.class.getResourceAsStream(resource)) {
            RunScript.execute(connection, new InputStreamReader(input, StandardCharsets.UTF_8));
        }
    }

    void sql(String sql) throws Exception {
        try (Connection connection = this.source.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    static Map<FieldDef, String> info(String method, String path) {
        Map<FieldDef, String> fields = new EnumMap<>(FieldDef.class);
        fields.put(METHOD, method);
        fields.put(PATH, path);
        fields.put(STATUS, "0");
        fields.put(COMMENT, "test");
        fields.put(TYPE, "DataQL");
        fields.put(SCRIPT, "return 'original';");
        fields.put(SCHEMA, "{}");
        fields.put(SAMPLE, "{}");
        fields.put(OPTION, "{}");
        fields.put(CREATE_TIME, "1");
        fields.put(GMT_TIME, "1");
        return fields;
    }

    static Map<FieldDef, String> release(String apiID) {
        Map<FieldDef, String> fields = info("GET", "/same-path");
        fields.remove(CREATE_TIME);
        fields.remove(GMT_TIME);
        fields.put(API_ID, apiID);
        fields.put(RELEASE_TIME, "2");
        return fields;
    }

    @Override
    public void close() throws Exception {
        this.sql("SHUTDOWN");
    }
}
