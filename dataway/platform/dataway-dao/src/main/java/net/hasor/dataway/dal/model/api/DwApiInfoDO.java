/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.model.api;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import net.hasor.dbvisitor.mapping.Column;
import net.hasor.dbvisitor.mapping.KeyType;
import net.hasor.dbvisitor.mapping.Table;

@Table(value = "dw_interface_info", mapUnderscoreToCamelCase = true)
@Getter
@Setter
public class DwApiInfoDO {
    @Column(primary = true, keyType = KeyType.Auto)
    private Long   id;
    private Date   gmtCreate;
    private Date   gmtModified;
    private String apiId;
    private String apiMethod;
    private String apiPath;
    private String apiStatus;
    private String apiComment;
    private String apiType;
    private String apiScript;
    private String apiSchema;
    private String apiSample;
    private String apiOption;
    private Long   apiCreateTime;
    private Long   apiGmtTime;
}
