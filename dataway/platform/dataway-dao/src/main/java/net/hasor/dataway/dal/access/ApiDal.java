/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.access;
import net.hasor.dataway.dal.mapper.api.DwInterfaceInfoMapper;
import net.hasor.dataway.dal.mapper.api.DwInterfaceHistoryMapper;

public interface ApiDal {
    DwInterfaceInfoMapper dwInterfaceInfoMapper();

    DwInterfaceHistoryMapper dwInterfaceHistoryMapper();
}
