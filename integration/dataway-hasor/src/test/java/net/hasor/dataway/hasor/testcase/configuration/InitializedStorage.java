/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase.configuration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import net.hasor.core.Init;
import net.hasor.core.Inject;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.DataMutation;
import net.hasor.dataway.dal.EntityType;
import net.hasor.dataway.dal.FieldDef;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;

public class InitializedStorage implements ApiDataAccessLayer {
    @Inject
    private DataSource          source;
    private JdbcDataAccessLayer delegate;

    @Init
    public void initialize() {
        this.delegate = new JdbcDataAccessLayer(this.source);
    }

    @Override
    public void configureMapping(Map<EntityType, String> tables, Map<EntityType, Map<FieldDef, String>> fields) {
        this.delegate.configureMapping(tables, fields);
    }

    @Override
    public List<Map<FieldDef, String>> listObjects(EntityType type, Map<FieldDef, String> conditions) {
        return this.delegate.listObjects(type, conditions);
    }

    @Override
    public void write(List<DataMutation> mutations) {
        this.delegate.write(mutations);
    }
}
