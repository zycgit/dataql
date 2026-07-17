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
