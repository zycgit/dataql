package net.hasor.dataql.sqlproc;
import net.hasor.dataql.domain.HintValue;

public interface SqlHintValue extends HintValue {
    /**
     * SqlFragment 的内容包含 Xml 片段。
     */
    String FRAGMENT_FORMAT_XML                         = "xml";
    /**
     * SqlFragment 的内容为文本片段。
     */
    String FRAGMENT_FORMAT_TEXT                        = "text";
    /**
     * SqlFragment 在执行 SQL 时使用 java.sql.Statement
     */
    String FRAGMENT_SQL_STAT_TYPE                      = "statement";
    /**
     * SqlFragment 在执行 SQL 时使用 java.sql.PreparedStatement
     */
    String FRAGMENT_SQL_STAT_PREPARED                  = "prepared";
    /**
     * SqlFragment 在执行 SQL 时使用 java.sql.CallableStatement
     */
    String FRAGMENT_SQL_STAT_CALLABLE                  = "callable";
    /**
     * SqlFragment 在执行 SQL 时要求的结果集返回方式为默认，不指定
     */
    String FRAGMENT_SQL_STAT_TYPE_DEFAULT              = "";
    /**
     * SqlFragment 在执行 SQL 时要求的结果集返回方式为 ResultSet.TYPE_FORWARD_ONLY
     */
    String FRAGMENT_SQL_STAT_RESULT_FORWARD_ONLY       = "forwardOnly";
    /**
     * SqlFragment 在执行 SQL 时要求的结果集返回方式为 ResultSet.TYPE_SCROLL_SENSITIVE
     */
    String FRAGMENT_SQL_STAT_RESULT_SCROLL_SENSITIVE   = "scrollSensitive";
    /**
     * SqlFragment 在执行 SQL 时要求的结果集返回方式为 ResultSet.TYPE_SCROLL_INSENSITIVE
     */
    String FRAGMENT_SQL_STAT_RESULT_SCROLL_INSENSITIVE = "scrollInsensitive";
    /**
     * SqlFragment 在执行 SQL 时要求的结果集返回方式为 默认，不设置
     */
    String FRAGMENT_SQL_STAT_RESULT_DEFAULT            = "default";
    /**
     * SqlFragment 用来决定 SQL 执行中使用了 selectKey 方式获取自增属性的执行时机是在 语句执行之前。
     */
    String FRAGMENT_SQL_STAT_ORDER_BEFORE              = "before";
    /**
     * SqlFragment 用来决定 SQL 执行中使用了 selectKey 方式获取自增属性的执行时机是在 语句执行之后。
     */
    String FRAGMENT_SQL_STAT_ORDER_AFTER               = "after";
    /**
     * SqlFragment 在执行 SQL 时获取由执行生成的自增属性 java.sql.Statement.getGeneratedKeys()
     */
    String FRAGMENT_SQL_STAT_KEY_GENERATED_ENABLE      = "true";
    /**
     * SqlFragment 在执行 SQL 时放弃执行可能生成的任何自增属性 java.sql.Statement.getGeneratedKeys()
     */
    String FRAGMENT_SQL_STAT_KEY_GENERATED_DISABLE     = "false";
    /**
     * SqlFragment 返回值不拆开，无论返回数据，都以 List/Map 形式返回。
     */
    String FRAGMENT_SQL_OPEN_PACKAGE_OFF               = "off";
    /**
     * SqlFragment 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。
     * - 当返回 0 或 1 条记录时，自动解开最外层的 List，返回一个 Object。
     */
    String FRAGMENT_SQL_OPEN_PACKAGE_ROW               = "row";
    /**
     * SqlFragment 返回值拆分到行，如果返回值是多条记录那么行为和 off 相同。
     * - 如果返回值是 1条记录并且具有多个字段值，那么行为和 row 相同。
     * - 一条记录中如果只有一个字段，那么会忽略字段名直接返回这个字段的值。
     * - 如果查询结果为空集合，那么返回 null 值。
     */
    String FRAGMENT_SQL_OPEN_PACKAGE_COLUMN            = "column";
    /**
     * SqlFragment 在执行 select 语句时采用分页模式执行，获取数据的步骤：1先获取查询对象，2.设置分页参数，3.获取分页之后的数据。
     */
    String FRAGMENT_SQL_QUERY_BY_PAGE_ENABLE           = "true";
    /**
     * SqlFragment 在执行 select 语句时不分页，获取数据的步骤：1先获取查询对象，2.获取SQL执行的数据。
     */
    String FRAGMENT_SQL_QUERY_BY_PAGE_DISABLE          = "false";
    /**
     * SqlFragment 返回的列信息,全部列名保持大小写敏感。
     */
    String FRAGMENT_SQL_COLUMN_CASE_DEFAULT            = "default";
    /**
     * SqlFragment 全部列名保持大写，如果在转换过程中发生冲突，那么会产生覆盖问题。
     */
    String FRAGMENT_SQL_COLUMN_CASE_UPPER              = "upper";
    /**
     * SqlFragment 全部列名保持小写，如果在转换过程中发生冲突，那么会产生覆盖问题。
     */
    String FRAGMENT_SQL_COLUMN_CASE_LOWER              = "lower";
    /**
     * SqlFragment 返回的列信息,全部列名做一次驼峰转换。如：goods_id => goodsId、GOODS_id => goodsId。
     */
    String FRAGMENT_SQL_COLUMN_CASE_HUMP               = "hump";
}