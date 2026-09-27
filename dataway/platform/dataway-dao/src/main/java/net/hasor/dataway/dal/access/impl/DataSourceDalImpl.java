/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.access.impl;
import net.hasor.dataway.dal.access.DataSourceDal;
import net.hasor.dataway.dal.mapper.datasource.DwDsConfigMapper;
import net.hasor.dataway.dal.mapper.datasource.DwDsMapper;

public class DataSourceDalImpl implements DataSourceDal {
    private final DwDsMapper       dataSourceMapper;
    private final DwDsConfigMapper configMapper;

    public DataSourceDalImpl(DwDsMapper dataSourceMapper, DwDsConfigMapper configMapper) {
        this.dataSourceMapper = dataSourceMapper;
        this.configMapper = configMapper;
    }

    @Override
    public DwDsMapper dwDsMapper() {
        return this.dataSourceMapper;
    }

    @Override
    public DwDsConfigMapper dwDsConfigMapper() {
        return this.configMapper;
    }
}
