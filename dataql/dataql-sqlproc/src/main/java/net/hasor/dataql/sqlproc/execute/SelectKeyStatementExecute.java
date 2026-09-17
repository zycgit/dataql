/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.execute;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.convert.ConverterBean;
import net.hasor.cobble.ref.BeanMap;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.SelectKeyConfig;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;

/**
 * 负责处理 SelectKey 的执行
 * @author 赵永春 (zyc@hasor.net)
 * @version 2021-11-05
 */
class SelectKeyStatementExecute {
    private final SelectKeyConfig          config;
    private final AbstractStatementExecute execute;

    SelectKeyStatementExecute(SelectKeyConfig config, AbstractStatementExecute execute) {
        this.config = config;
        this.execute = execute;
    }

    public void processBefore(Connection conn, Hints hints, Map<String, Object> parameter, List<SqlExecutionInterceptor> interceptors) throws SQLException {
        if (StringUtils.equalsIgnoreCase("BEFORE", this.config.getOrder())) {
            this.processSelectKey(conn, hints, parameter, interceptors);
        }
    }

    public void processAfter(Connection conn, Hints hints, Map<String, Object> parameter, List<SqlExecutionInterceptor> interceptors) throws SQLException {
        if (StringUtils.equalsIgnoreCase("AFTER", this.config.getOrder())) {
            this.processSelectKey(conn, hints, parameter, interceptors);
        }
    }

    private void processSelectKey(Connection conn, Hints hints, Map<String, Object> parameter, List<SqlExecutionInterceptor> interceptors) throws SQLException {
        String keyColumn = this.config.getKeyColumn();
        String keyProperty = this.config.getKeyProperty();
        Object resultValue = this.execute.execute(conn, hints, this.config, parameter, null, false, interceptors);

        if (resultValue instanceof List) {
            resultValue = ((List<?>) resultValue).get(0);
        }

        if (StringUtils.isNotBlank(keyColumn)) {
            String[] properties = keyProperty.split(",");
            String[] columns = keyColumn.split(",");
            if (properties.length != columns.length) {
                throw new SQLException("SelectKey keyProperty size " + properties.length + " and keyColumn size " + columns.length + ", mismatch.");
            }

            Map<String, Object> keyResult = null;
            if (resultValue instanceof Map) {
                keyResult = (Map<String, Object>) resultValue;
            } else {
                BeanMap beanMap = new BeanMap(resultValue);
                beanMap.setTransformConvert(ConverterBean.getInstance());
                keyResult = beanMap;
            }
            for (int i = 0; i < columns.length; i++) {
                parameter.put(properties[i], keyResult.get(columns[i]));
            }

        } else {
            String[] properties = keyProperty.split(",");
            if (properties.length > 1) {
                throw new SQLException("SelectKey multiple property, keyColumn must be config.");
            }

            if (resultValue instanceof Map) {
                resultValue = ((Map<?, ?>) resultValue).values().stream().findFirst().orElse(null);
            }

            parameter.put(properties[0], resultValue);
        }
    }
}
