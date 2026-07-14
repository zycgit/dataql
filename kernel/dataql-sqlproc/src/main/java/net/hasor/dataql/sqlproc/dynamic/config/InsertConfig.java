/*
 * Copyright 2015-2022 the original author or authors.
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
package net.hasor.dataql.sqlproc.dynamic.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.logic.ArrayDynamicSql;

/**
 * Insert SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class InsertConfig extends DmlConfig {
    private SelectKeyConfig selectKey;
    private boolean         useGeneratedKeys;
    private String          keyProperty;
    private String          keyColumn;

    public InsertConfig(ArrayDynamicSql target, Hints config) {
        super(target, config);

        if (config != null) {
            String generated = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_KEY_GENERATED);

            this.useGeneratedKeys = StringUtils.isNotBlank(generated) && Boolean.parseBoolean(generated);
            this.keyProperty = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_KEY_PROPERTY);
            this.keyColumn = SqlHintNames.getValue(config, SqlHintNames.FRAGMENT_SQL_KEY_COLUMN);
        }
    }

    @Override
    public QueryType getType() {
        return QueryType.Insert;
    }

    public SelectKeyConfig getSelectKey() {
        return this.selectKey;
    }

    public void setSelectKey(SelectKeyConfig selectKey) {
        this.selectKey = selectKey;
    }

    public boolean isUseGeneratedKeys() {
        return this.useGeneratedKeys;
    }

    public String getKeyColumn() {
        return this.keyColumn;
    }

    public String getKeyProperty() {
        return this.keyProperty;
    }
}