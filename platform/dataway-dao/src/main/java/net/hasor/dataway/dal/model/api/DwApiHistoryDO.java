/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
