/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.access;
import net.hasor.dataway.dal.mapper.auth.DwAuthRoleMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthUserMapper;

public interface AuthDal {
    DwAuthUserMapper dwAuthUserMapper();

    DwAuthRoleMapper dwAuthRoleMapper();
}
