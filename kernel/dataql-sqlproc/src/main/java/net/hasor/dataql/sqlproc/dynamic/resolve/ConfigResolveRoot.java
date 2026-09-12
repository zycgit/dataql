/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.resolve;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public class ConfigResolveRoot {
    private final ConfigResolveByPlainSql planSql = new ConfigResolveByPlainSql();
    private final ConfigResolveByXmlSql   xmlSql  = new ConfigResolveByXmlSql();

    public SqlConfig parseXmlConfig(String fragmentName, Hints hint, String config) {
        return this.xmlSql.parseConfig(fragmentName, hint, config);
    }

    public SqlConfig parsePlainConfig(String fragmentName, Hints hint, String config) {
        return this.planSql.parseConfig(fragmentName, hint, config);
    }
}
