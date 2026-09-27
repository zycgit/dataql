/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.authorization;
import net.hasor.cobble.StringUtils;

/**
 * 界面操作权限点。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2020-06-03
 */
public enum PermissionType {
    /** UI - 单个API信息查阅 */
    ApiInfo("api_info"),
    /** UI - 查看API列表 */
    ApiList("api_list"),
    /** UI- 查看API的发布历史列表 */
    ApiHistory("api_history"),
    /** UI- API的编辑和保存操作 */
    ApiEdit("api_edit"),
    /** UI- 在编辑页面执行API */
    ApiPerform("api_perform"),
    /** UI- 接口发布能力(仅:冒烟/发布) */
    ApiPublish("api_publish"),
    /** UI- 已发布接口进行禁用 or 下线 */
    ApiDisable("api_disable"),
    /** UI- 删除一个接口 */
    ApiDelete("api_delete"),
    /** 接口发布之后的调用 */
    ApiExecute("api_execute"),
    ;
    private final String permissionCode;

    PermissionType(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionCode() {
        return this.permissionCode;
    }

    public static PermissionType ofPermissionCode(String permissionCode) {
        for (PermissionType permissionType : PermissionType.values()) {
            if (StringUtils.equalsIgnoreCase(permissionCode, permissionType.permissionCode)) {
                return permissionType;
            }
        }
        return null;
    }
}
