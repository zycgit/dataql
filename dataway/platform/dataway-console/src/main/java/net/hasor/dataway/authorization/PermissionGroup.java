/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import net.hasor.cobble.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static net.hasor.dataway.authorization.PermissionType.*;

/**
 * 权限分组。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-03
 */
public enum PermissionGroup {
    /** 分组:全部 */
    Group_Full(new PermissionType[] { ApiInfo, ApiList, ApiHistory, ApiEdit, ApiPublish, ApiDisable, ApiDelete, ApiExecute }),
    /** 分组:只读 */
    Group_ReadOnly(new PermissionType[] { ApiList, ApiInfo, ApiHistory }),
    /** 分组:仅执行 */
    Group_Execute(new PermissionType[] { ApiExecute, ApiPerform }),
    ;
    private final PermissionType[] permissionTypes;

    PermissionGroup(PermissionType[] permissionTypes) {
        this.permissionTypes = (permissionTypes == null ? new PermissionType[0] : permissionTypes);
    }

    /** 权限 Code 集 */
    public Set<String> toCodeSet() {
        return Arrays.stream(this.permissionTypes).map(PermissionType::getPermissionCode).collect(Collectors.toSet());
    }

    /** 执行检测权限 */
    public boolean testPermission(PermissionType permissionType) {
        for (PermissionType type : this.permissionTypes) {
            if (type == permissionType) {
                return true;
            }
        }
        return false;
    }

    /** 执行检测权限 */
    public boolean testPermission(String permissionCode) {
        for (PermissionType type : this.permissionTypes) {
            if (StringUtils.equals(type.getPermissionCode(), permissionCode)) {
                return true;
            }
        }
        return false;
    }
}
