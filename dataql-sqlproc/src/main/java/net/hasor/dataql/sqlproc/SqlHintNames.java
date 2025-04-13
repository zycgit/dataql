/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.sqlproc;

import net.hasor.dataql.Hints;
import static net.hasor.dataql.sqlproc.SqlHintValue.*;

/**
 * Hint 的 keys 定义。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-03-23
 */
public enum SqlHintNames {
    /** SQL 的格式，Xml 或 text（Xml 格式为包含 XML 动态 SQL 标签的 字符串片段） */
    FRAGMENT_SQL_FORMAT(FRAGMENT_FORMAT_TEXT),
    /** 在执行 SQL 时使用的方式 statement、prepared、callable */
    FRAGMENT_SQL_STATEMENT(FRAGMENT_SQL_STAT_PREPARED, "statementType"),
    /** 在执行 SQL 时查询超时时间，默认 -1 表示不设置 */
    FRAGMENT_SQL_TIMEOUT("-1", "timeout"),
    /** 在执行 SQL 后获取结果集时候设置的 fetchSize */
    FRAGMENT_SQL_FETCH_SIZE("256", "fetchSize"),
    /** 在执行 SQL 时要求的结果集返回方式为 */
    FRAGMENT_SQL_RESULT_SET_TYPE(FRAGMENT_SQL_STAT_TYPE_DEFAULT, "resultSetType"),
    /** 对输出参数进行过滤，用来确定哪些结果参数会被接收，如果不指定将会接收所有参数。 */
    FRAGMENT_SQL_BIND_OUT(null, "bindOut"),
    /** 在执行 SQL 后是否获取由执行生成的任何自增属性 */
    FRAGMENT_SQL_KEY_GENERATED(FRAGMENT_SQL_STAT_KEY_GENERATED_DISABLE, "useGeneratedKeys"),
    /** 配合 selectKey 一起使用，用来决定获取的自增列名在回写对象时的属性名。*/
    FRAGMENT_SQL_KEY_PROPERTY(null, "keyProperty"),
    /** 配合 selectKey 一起使用返回结果集中与属性匹配的列名，如果需要选择多个列，可以使用逗号分割属性名列表。列名和属性名的顺序一致。  */
    FRAGMENT_SQL_KEY_COLUMN(null, "keyColumn"),
    /** 用来决定 SQL 执行中使用了 selectKey 方式获取自增属性的执行时机 */
    FRAGMENT_SQL_ORDER(FRAGMENT_SQL_STAT_ORDER_AFTER, "order"),

    //

    /** SqlFragment 查询执行是否使用分页模式（默认：不使用） */
    FRAGMENT_SQL_QUERY_BY_PAGE(FRAGMENT_SQL_QUERY_BY_PAGE_DISABLE),
    /**
     * SqlFragment 在执行分页查询时，设置的当前页码偏移量。原始的 currentPage 规定启始页码是从 0 开始。在某些场景下 1开始会比较好理解，这时候就可以设施偏移量 为 1。
     * <p>当设置偏移量之后，真实的 currentPage 值计算方式为：<code>yourCurrentPage - FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET</code> 结果如果小于等于0，那么设置为 0</p>
     */
    FRAGMENT_SQL_QUERY_BY_PAGE_NUMBER_OFFSET("0"),
    /** SqlFragment 分页查询在改写分页查询语句时使用的方言（默认：空，需要明确指定） */
    FRAGMENT_SQL_PAGE_DIALECT(""),

    //

    /** SqlFragment 返回值，拆开方式 */
    FRAGMENT_SQL_OPEN_PACKAGE(FRAGMENT_SQL_OPEN_PACKAGE_COLUMN),
    /** SqlFragment 返回的列信息大小写模式：default、upper、lower、hump */
    FRAGMENT_SQL_COLUMN_CASE(FRAGMENT_SQL_COLUMN_CASE_DEFAULT),

    //

    /** SqlFragment 数据源名字 */
    FRAGMENT_SQL_DATA_SOURCE(""),
    /** SqlFragment 当遇到多条 SQL 同时执行时，结果集的行为（默认：返回最后一个结果） */
    FRAGMENT_SQL_MULTIPLE_QUERIES(FRAGMENT_SQL_MULTIPLE_QUERIES_LAST),
    ;

    private final String shortName;
    private final String defaultVal;

    public String getShortName() {
        return this.shortName;
    }

    public String getDefaultVal() {
        return this.defaultVal;
    }

    SqlHintNames(String defaultVal) {
        this(null, defaultVal);
    }

    SqlHintNames(String defaultVal, String shortName) {
        this.shortName = shortName;
        this.defaultVal = defaultVal;
    }

    public static String getValue(Hints hint, SqlHintNames hintName) {
        Object value = hint.getHint(hintName.getShortName());
        if (value == null) {
            return (String) hint.getOrDefault(hintName.name(), hintName.getDefaultVal());
        } else {
            return (String) value;
        }
    }
}