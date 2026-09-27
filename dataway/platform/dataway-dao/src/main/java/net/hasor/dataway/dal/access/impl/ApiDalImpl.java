/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.access.impl;
import net.hasor.dataway.dal.access.ApiDal;
import net.hasor.dataway.dal.mapper.api.DwInterfaceHistoryMapper;
import net.hasor.dataway.dal.mapper.api.DwInterfaceInfoMapper;

public class ApiDalImpl implements ApiDal {
    private final DwInterfaceInfoMapper    infoMapper;
    private final DwInterfaceHistoryMapper historyMapper;

    public ApiDalImpl(DwInterfaceInfoMapper infoMapper, DwInterfaceHistoryMapper historyMapper) {
        this.infoMapper = infoMapper;
        this.historyMapper = historyMapper;
    }

    @Override
    public DwInterfaceInfoMapper dwInterfaceInfoMapper() {
        return this.infoMapper;
    }

    @Override
    public DwInterfaceHistoryMapper dwInterfaceHistoryMapper() {
        return this.historyMapper;
    }
}
