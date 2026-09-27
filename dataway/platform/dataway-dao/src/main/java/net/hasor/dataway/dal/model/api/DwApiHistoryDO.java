/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.model.api;

import lombok.Getter;
import lombok.Setter;
import net.hasor.dbvisitor.mapping.Column;
import net.hasor.dbvisitor.mapping.Table;

@Table(value = "dw_interface_history", mapUnderscoreToCamelCase = true)
@Getter
@Setter
public class DwApiHistoryDO {
    @Column(primary = true)
    private String historyId;
    private String historyApiId;
    private String historyMethod;
    private String historyPath;
    private String historyStatus;
    private String historyComment;
    private String historyType;
    private String historyScript;
    private String historyScriptOri;
    private String historySchema;
    private String historySample;
    private String historyOption;
    private Long historyCreateTime;
    @Column("is_release")
    private Boolean release;
}
