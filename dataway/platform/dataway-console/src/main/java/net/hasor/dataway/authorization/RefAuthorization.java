/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 负责UI界面调用的权限判断。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-03
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RefAuthorization {
    /** 标定点权限 */
    PermissionType value();
}
