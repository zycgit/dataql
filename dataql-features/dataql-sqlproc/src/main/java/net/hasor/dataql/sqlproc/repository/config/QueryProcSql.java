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
package net.hasor.dataql.sqlproc.repository.config;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.sqlproc.repository.DynamicSql;
import net.hasor.dataql.sqlproc.repository.MultipleResultsType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * All DML SqlConfig
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-19
 */
public class QueryProcSql extends AbstractProcSql {
    // insert
    private boolean             useGeneratedKeys = false;
    private String              keyProperty      = null;
    // query
    private int                 fetchSize;
    private MultipleResultsType multipleResultType;

    public QueryProcSql(DynamicSql target) {
        super(target);
    }

    public QueryProcSql(DynamicSql target, Node operationNode) {
        super(target, operationNode);
        NamedNodeMap nodeAttributes = operationNode.getAttributes();
        Node fetchSizeNode = nodeAttributes.getNamedItem("fetchSize");
        Node multipleResultNode = nodeAttributes.getNamedItem("multipleResult");
        String fetchSize = (fetchSizeNode != null) ? fetchSizeNode.getNodeValue() : null;
        String multipleResult = (multipleResultNode != null) ? multipleResultNode.getNodeValue() : null;

        this.fetchSize = StringUtils.isBlank(fetchSize) ? 256 : Integer.parseInt(fetchSize);
        this.multipleResultType = MultipleResultsType.valueOfCode(multipleResult, defaultMultipleResultsType());

        // 1st: SelectKey
        if (this.getSelectKey() == null) {
            Node useGeneratedKeysNode = nodeAttributes.getNamedItem("useGeneratedKeys");
            Node keyPropertyNode = nodeAttributes.getNamedItem("keyProperty");
            String useGeneratedKeys = (useGeneratedKeysNode != null) ? useGeneratedKeysNode.getNodeValue() : null;
            String keyProperty = (keyPropertyNode != null) ? keyPropertyNode.getNodeValue() : null;

            // 2st: useGeneratedKeys & keyProperty
            this.useGeneratedKeys = StringUtils.equalsIgnoreCase(useGeneratedKeys, "true");
            this.keyProperty = StringUtils.isBlank(keyProperty) ? null : keyProperty;
        }
    }

    protected MultipleResultsType defaultMultipleResultsType() {
        return MultipleResultsType.LAST;
    }

    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    public void setUseGeneratedKeys(boolean useGeneratedKeys) {
        this.useGeneratedKeys = useGeneratedKeys;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public void setKeyProperty(String keyProperty) {
        this.keyProperty = keyProperty;
    }

    public int getFetchSize() {
        return this.fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public MultipleResultsType getMultipleResultType() {
        return this.multipleResultType;
    }

    public void setMultipleResultType(MultipleResultsType multipleResultType) {
        this.multipleResultType = multipleResultType;
    }
}
