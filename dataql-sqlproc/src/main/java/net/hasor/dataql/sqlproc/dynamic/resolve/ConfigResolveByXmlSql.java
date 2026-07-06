/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.dynamic.resolve;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.hasor.dataql.Hints;
import net.hasor.dataql.runtime.HintsSet;
import net.hasor.dataql.sqlproc.SqlHintNames;
import net.hasor.dataql.sqlproc.dynamic.config.InsertConfig;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SelectKeyConfig;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.dynamic.logic.*;
import net.hasor.dataql.sqlproc.dynamic.segment.PlanDynamicSql;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * parse dynamic SQL from mapperFile
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2021-06-05
 */
public class ConfigResolveByXmlSql extends ConfigResolve {
    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    public static Document loadXmlRoot(Reader reader) throws ParserConfigurationException, IOException, SAXException {
        if (reader == null) {
            throw new NullPointerException("stream is null.");
        }
        DocumentBuilder documentBuilder = FACTORY.newDocumentBuilder();
        return documentBuilder.parse(new InputSource(reader));
    }

    @Override
    public SqlConfig parseConfig(String fragmentName, Hints hint, String config) {
        QueryType queryType = QueryType.valueOfTag(fragmentName.toLowerCase().trim());
        if (queryType == null) {
            throw new UnsupportedOperationException("fragment '" + fragmentName + "' Unsupported.");
        }
        try {
            Document document = loadXmlRoot(new StringReader("<root>" + config + "</root>"));
            NodeList rootNodes = document.getDocumentElement().getChildNodes();

            ArrayDynamicSql dynamicSql = new ArrayDynamicSql();
            parseNodeList(dynamicSql, rootNodes);

            SqlConfig sqlConfig = super.createConfig(queryType, hint, dynamicSql);

            // find selectKey
            if (sqlConfig instanceof InsertConfig) {
                InsertConfig insertConfig = (InsertConfig) sqlConfig;
                SelectKeyConfig keyConfig = null;
                for (int i = 0, len = rootNodes.getLength(); i < len; i++) {
                    Node node = rootNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE && "selectKey".equalsIgnoreCase(node.getNodeName())) {
                        keyConfig = parseSelectKeySqlNode(node);
                    }
                }
                insertConfig.setSelectKey(keyConfig);
            }
            return sqlConfig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getNodeAttributeValue(Node node, String attributeKey) {
        Node item = node.getAttributes().getNamedItem(attributeKey);
        return item != null ? item.getNodeValue() : null;
    }

    protected void parseNodeList(ArrayDynamicSql parentSqlNode, NodeList nodeList) {
        for (int i = 0, len = nodeList.getLength(); i < len; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                parseTextSqlNode(parentSqlNode, node);
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = node.getNodeName();
                if ("foreach".equalsIgnoreCase(nodeName)) {
                    parseForeachSqlNode(parentSqlNode, node);
                } else if ("if".equalsIgnoreCase(nodeName)) {
                    parseIfSqlNode(parentSqlNode, node);
                } else if ("trim".equalsIgnoreCase(nodeName)) {
                    parseTrimSqlNode(parentSqlNode, node);
                } else if ("where".equalsIgnoreCase(nodeName)) {
                    parseWhereSqlNode(parentSqlNode, node);
                } else if ("set".equalsIgnoreCase(nodeName)) {
                    parseSetSqlNode(parentSqlNode, node);
                } else if ("bind".equalsIgnoreCase(nodeName)) {
                    parseBindSqlNode(parentSqlNode, node);
                } else if ("choose".equalsIgnoreCase(nodeName)) {
                    parseChooseSqlNode(parentSqlNode, node);
                } else if ("when".equalsIgnoreCase(nodeName)) {
                    parseWhenSqlNode(parentSqlNode, node);
                } else if ("otherwise".equalsIgnoreCase(nodeName)) {
                    parseOtherwiseSqlNode(parentSqlNode, node);
                } else if ("include".equalsIgnoreCase(nodeName)) {
                    parseIncludeSqlNode(parentSqlNode, node);
                } else if ("selectKey".equalsIgnoreCase(nodeName)) {
                    //skip and Skip special treatment.
                } else {
                    throw new UnsupportedOperationException("Unsupported tags :" + nodeName);
                }
            } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
                parseTextSqlNode(parentSqlNode, node);
            }
        }
    }

    /** append text */
    protected void parseTextSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String sqlNode = curXmlNode.getNodeValue();
        if (parentSqlNode.lastIsText()) {
            ((PlanDynamicSql) parentSqlNode.lastNode()).parsedAppend(sqlNode);
        } else {
            parentSqlNode.addChildNode(new PlanDynamicSql(sqlNode));
        }
    }

    /** passer &lt;foreach&gt; xmlNode */
    protected void parseForeachSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String collection = getNodeAttributeValue(curXmlNode, "collection");
        String item = getNodeAttributeValue(curXmlNode, "item");
        String open = getNodeAttributeValue(curXmlNode, "open");
        String close = getNodeAttributeValue(curXmlNode, "close");
        String separator = getNodeAttributeValue(curXmlNode, "separator");

        ArrayDynamicSql parent = new ForeachDynamicSql(collection, item, open, close, separator);
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;if&gt; xmlNode */
    protected void parseIfSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String test = getNodeAttributeValue(curXmlNode, "test");

        ArrayDynamicSql parent = new IfDynamicSql(test);
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;trim&gt; xmlNode */
    protected void parseTrimSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String prefix = getNodeAttributeValue(curXmlNode, "prefix");
        String prefixOverrides = getNodeAttributeValue(curXmlNode, "prefixOverrides");
        String suffix = getNodeAttributeValue(curXmlNode, "suffix");
        String suffixOverrides = getNodeAttributeValue(curXmlNode, "suffixOverrides");

        ArrayDynamicSql parent = new TrimDynamicSql(prefix, suffix, prefixOverrides, suffixOverrides);
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;where&gt; xmlNode */
    protected void parseWhereSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        ArrayDynamicSql parent = new WhereDynamicSql();
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;set&gt; xmlNode */
    protected void parseSetSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {

        ArrayDynamicSql parent = new SetDynamicSql();
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;bind&gt; xmlNode */
    protected void parseBindSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String name = getNodeAttributeValue(curXmlNode, "name");
        String value = getNodeAttributeValue(curXmlNode, "value");

        parentSqlNode.addChildNode(new BindDynamicSql(name, value));
    }

    /** passer &lt;choose&gt; xmlNode */
    protected void parseChooseSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        ArrayDynamicSql parent = new ChooseDynamicSql();
        parentSqlNode.addChildNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;when&gt; xmlNode */
    protected void parseWhenSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        if (!(parentSqlNode instanceof ChooseDynamicSql)) {
            throw new UnsupportedOperationException("the tag `<when>` parent tag must be `<choose>`");
        }
        String test = getNodeAttributeValue(curXmlNode, "test");
        ChooseDynamicSql chooseSqlNode = (ChooseDynamicSql) parentSqlNode;

        ArrayDynamicSql parent = new ArrayDynamicSql();
        chooseSqlNode.addThen(test, parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;otherwise&gt; xmlNode */
    protected void parseOtherwiseSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        if (!(parentSqlNode instanceof ChooseDynamicSql)) {
            throw new UnsupportedOperationException("the tag `<otherwise>` parent tag must be `<choose>`");
        }
        ChooseDynamicSql chooseSqlNode = (ChooseDynamicSql) parentSqlNode;

        ArrayDynamicSql parent = new ArrayDynamicSql();
        chooseSqlNode.setDefaultNode(parent);
        this.parseNodeList(parent, curXmlNode.getChildNodes());
    }

    /** passer &lt;include&gt; xmlNode */
    protected void parseIncludeSqlNode(ArrayDynamicSql parentSqlNode, Node curXmlNode) {
        String refId = getNodeAttributeValue(curXmlNode, "refid");
        parentSqlNode.addChildNode(new MacroDynamicSql(refId));
    }

    /** passer &lt;selectKey&gt; xmlNode */
    private SelectKeyConfig parseSelectKeySqlNode(Node curXmlNode) {
        Hints cfg = new HintsSet();
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_STATEMENT.getShortName(), getNodeAttributeValue(curXmlNode, "statementType"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_TIMEOUT.getShortName(), getNodeAttributeValue(curXmlNode, "timeout"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_FETCH_SIZE.getShortName(), getNodeAttributeValue(curXmlNode, "fetchSize"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_RESULT_SET_TYPE.getShortName(), getNodeAttributeValue(curXmlNode, "resultSetType"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_KEY_PROPERTY.getShortName(), getNodeAttributeValue(curXmlNode, "keyProperty"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_KEY_COLUMN.getShortName(), getNodeAttributeValue(curXmlNode, "keyColumn"));
        cfg.setHint(SqlHintNames.FRAGMENT_SQL_ORDER.getShortName(), getNodeAttributeValue(curXmlNode, "order"));

        ArrayDynamicSql parent = new ArrayDynamicSql();
        this.parseNodeList(parent, curXmlNode.getChildNodes());

        return new SelectKeyConfig(parent, cfg);
    }
}