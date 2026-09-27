/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.access.impl;
import net.hasor.dataway.dal.access.AuthDal;
import net.hasor.dataway.dal.mapper.auth.DwAuthRoleMapper;
import net.hasor.dataway.dal.mapper.auth.DwAuthUserMapper;

public class AuthDalImpl implements AuthDal {
    private final DwAuthUserMapper userMapper;
    private final DwAuthRoleMapper roleMapper;

    public AuthDalImpl(DwAuthUserMapper userMapper, DwAuthRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public DwAuthUserMapper dwAuthUserMapper() {
        return this.userMapper;
    }

    @Override
    public DwAuthRoleMapper dwAuthRoleMapper() {
        return this.roleMapper;
    }
}
