/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.jdbc;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import net.hasor.dataway.dal.*;
import net.hasor.dbvisitor.jdbc.DynamicConnection;
import net.hasor.dbvisitor.lambda.LambdaTemplate;
import net.hasor.dbvisitor.lambda.MapQuery;
import net.hasor.dbvisitor.lambda.MapUpdate;

/** Legacy table storage through dbVisitor. Connection ownership and transaction boundaries are supplied by JdbcExecutor. */
public class JdbcDataAccessLayer implements ApiDataAccessLayer {
    private final JdbcExecutor                   dbExecutor;
    private final LambdaTemplate                 lambda;
    private final ThreadLocal<Connection>        current  = new ThreadLocal<>();
    private final Map<EntityType, EntityMapping> mappings = new EnumMap<>(EntityType.class);

    public JdbcDataAccessLayer(DataSource source, String tablePrefix) {
        this(new LocalJdbcExecutor(source), tablePrefix);
    }

    public JdbcDataAccessLayer(JdbcExecutor dbExecutor, String tablePrefix) {
        this.dbExecutor = Objects.requireNonNull(dbExecutor);
        if (tablePrefix == null || !tablePrefix.matches("[a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Invalid table prefix");
        }

        for (EntityType entityType : EntityType.values()) {
            mappings.put(entityType, new EntityMapping(entityType, tablePrefix));
        }

        try {
            this.lambda = new LambdaTemplate(new DynamicConnection() {
                @Override
                public Connection getConnection() throws SQLException {
                    Connection connection = current.get();
                    if (connection == null) {
                        throw new SQLException("Database access requires an executor callback");
                    }
                    return connection;
                }

                @Override
                public void releaseConnection(Connection connection) {
                    // The executor owns this connection; dbVisitor only borrows it.
                }
            });
        } catch (SQLException e) {
            throw new DataAccessException("Cannot initialize Dataway database access", e);
        }
    }

    @Override
    public List<Map<FieldDef, String>> listObjects(EntityType entityType, Map<FieldDef, String> conditions) {
        EntityMapping mapping = findEntityMapping(entityType);
        conditions.keySet().forEach(mapping::column);
        try {
            return execute(connection -> {
                MapQuery query = this.lambda.queryFreedom(mapping.table());
                query.select(mapping.columns().values().toArray(String[]::new));
                conditions.forEach((field, value) -> {
                    if (value == null) {
                        query.isNull(mapping.column(field));
                    } else {
                        query.eq(mapping.column(field), value);
                    }
                });

                query.orderBy(mapping.column(FieldDef.ID));
                return query.queryForList((rs, row) -> {
                    Map<FieldDef, String> values = new EnumMap<>(FieldDef.class);
                    for (var entry : mapping.columns().entrySet()) {
                        values.put(entry.getKey(), rs.getString(entry.getValue()));
                    }
                    return values;
                });
            });
        } catch (SQLException e) {
            throw new DataAccessException("Cannot query Dataway storage", e);
        }
    }

    @Override
    public void write(List<DataMutation> mutations) {
        if (mutations.isEmpty()) {
            return;
        }

        for (DataMutation mutation : mutations) {
            mutation.validate();
            EntityMapping mapping = findEntityMapping(mutation.getEntityType());
            for (FieldDef field : mutation.getFields().keySet()) {
                mapping.column(field);
            }
        }

        try {
            execute(connection -> {
                for (DataMutation mutation : mutations) {
                    EntityMapping mapping = findEntityMapping(mutation.getEntityType());
                    int count = switch (mutation.getOperationType()) {
                        case CREATE -> insert(mapping, mutation);
                        case UPDATE -> update(mapping, mutation);
                        case DELETE -> delete(mapping, mutation);
                    };
                    if (count != 1) {
                        throw new DataConflictException("Dataway record changed; reload and retry");
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            if (isDuplicate(e)) {
                throw new DataConflictException("Dataway record id or (method, path) already exists", e);
            }
            throw new DataAccessException("Cannot write Dataway storage", e);
        }
    }

    //

    private <T> T execute(JdbcCallback<T> callback) throws SQLException {
        return this.dbExecutor.execute(connection -> {
            Objects.requireNonNull(connection, "executor connection");
            Connection previous = this.current.get();
            this.current.set(connection);
            try {
                return callback.execute(connection);
            } finally {
                if (previous == null) {
                    current.remove();
                } else {
                    current.set(previous);
                }
            }
        });
    }

    private boolean isDuplicate(SQLException exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        pending.add(exception);
        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (current instanceof SQLException sql) {
                if ("23505".equals(sql.getSQLState()) || sql.getErrorCode() == 1062) {
                    return true;
                }
                if (sql.getNextException() != null) {
                    pending.add(sql.getNextException());
                }
            }
            if (current.getCause() != null) {
                pending.add(current.getCause());
            }
        }
        return false;
    }

    private EntityMapping findEntityMapping(EntityType entityType) {
        EntityMapping mapping = this.mappings.get(Objects.requireNonNull(entityType, "entityType"));
        if (mapping == null) {
            throw new IllegalArgumentException("Unsupported entity type: " + entityType);
        }

        return mapping;
    }

    //

    private int insert(EntityMapping mapping, DataMutation mutation) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(mapping.column(FieldDef.ID), mutation.getId());
        values.put(mapping.column(FieldDef.REVISION), 1L);
        mutation.getFields().forEach((field, value) -> {
            values.put(mapping.column(field), value);
        });

        return this.lambda.insertFreedom(mapping.table()).applyMap(values).executeSumResult();
    }

    private int update(EntityMapping mapping, DataMutation mutation) throws SQLException {
        String revisionColumn = mapping.column(FieldDef.REVISION);
        MapUpdate update = this.lambda.updateFreedom(mapping.table()) //
                .eq(mapping.column(FieldDef.ID), mutation.getId())  //
                .eq(revisionColumn, mutation.getVersion())          //
                .updateTo(revisionColumn, Math.addExact(mutation.getVersion(), 1));

        // updateTo preserves explicit nulls; updateToSample would silently skip them.
        mutation.getFields().forEach((field, value) -> {
            update.updateTo(mapping.column(field), value);
        });
        return update.doUpdate();
    }

    private int delete(EntityMapping mapping, DataMutation mutation) throws SQLException {
        return this.lambda.deleteFreedom(mapping.table())                      //
                .eq(mapping.column(FieldDef.ID), mutation.getId())           //
                .eq(mapping.column(FieldDef.REVISION), mutation.getVersion())//
                .doDelete();
    }
}
