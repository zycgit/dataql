package net.hasor.dataql.sqlproc.execute.support;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.function.EFunction;
import net.hasor.cobble.io.IOUtils;
import net.hasor.dataql.Hints;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;

/**
 * 通用 SQL FragmentProcess（@@execute），自动推断查询类型，同时支持 @@type 前缀显式覆盖。
 */
public class ExecuteFragmentProcess extends AbstractSqlFragment {
    public ExecuteFragmentProcess(EFunction<String, Connection, SQLException> c) {
        this("execute", c);
    }

    public ExecuteFragmentProcess(String fragmentCommand, EFunction<String, Connection, SQLException> c) {
        super(fragmentCommand, c);
    }

    @Override
    protected QueryType queryType(String fragmentString, Hints hints) {
        QueryType explicit = parseExplicitType(fragmentString);
        return explicit != null ? explicit : inferQueryType(fragmentString);
    }

    @Override
    protected SqlConfig buildConfig(String fragmentString, Hints hints) {
        return this.buildConfig("execute", fragmentString, hints);
    }

    @Override
    protected SqlConfig buildConfig(String fragmentCommand, String fragmentString, Hints hints) {
        if (parseExplicitType(fragmentString) != null) {
            return super.buildConfig(fragmentCommand, stripAtPrefix(fragmentString), hints);
        } else {
            return super.buildConfig(fragmentCommand, fragmentString, hints);
        }
    }

    // ----------------------------------------------------------------
    // Type inference / explicit type
    // ----------------------------------------------------------------

    public static QueryType parseExplicitType(String fragmentString) {
        if (fragmentString == null || !fragmentString.contains("@@")) {
            return null;
        }

        int idx = fragmentString.indexOf("@@");
        StringBuilder sb = new StringBuilder();
        for (int i = idx + 2; i < fragmentString.length() && Character.isJavaIdentifierPart(fragmentString.charAt(i)); i++) {
            sb.append(fragmentString.charAt(i));
        }

        return sb.isEmpty() ? null : QueryType.valueOfTag(sb.toString());
    }

    private static String stripAtPrefix(String s) {
        int idx = s.indexOf("@@");
        int end = idx + 2;
        while (end < s.length() && Character.isJavaIdentifierPart(s.charAt(end))) {
            end++;
        }

        return s.substring(end).stripLeading();
    }

    public static QueryType inferQueryType(String fragmentString) {
        List<String> lines;
        try {
            lines = IOUtils.readLines(new StringReader(fragmentString));
        } catch (IOException e) {
            return QueryType.Execute;
        }

        boolean inBlock = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (!inBlock) {
                if (StringUtils.isBlank(line))
                    continue;
                if (line.startsWith("--") || line.startsWith("//"))
                    continue;
                if (line.startsWith("/*")) {
                    if (line.contains("*/"))
                        line = line.substring(line.indexOf("*/") + 2).trim();
                    if (StringUtils.isBlank(line)) {
                        inBlock = true;
                        continue;
                    }
                }
            }
            if (inBlock) {
                if (line.contains("*/")) {
                    line = line.substring(line.indexOf("*/") + 2).trim();
                    inBlock = false;
                } else
                    continue;
            }
            if (StringUtils.isBlank(line))
                continue;

            String lower = line.toLowerCase();
            if (lower.startsWith("insert") || lower.startsWith("replace"))
                return QueryType.Insert;
            if (lower.startsWith("update"))
                return QueryType.Update;
            if (lower.startsWith("delete"))
                return QueryType.Delete;
            if (lower.startsWith("select") || lower.startsWith("with"))
                return QueryType.Select;
            if (lower.startsWith("call") || lower.startsWith("exec") || lower.startsWith("{call"))
                return QueryType.Call;
            return QueryType.Execute;
        }
        return QueryType.Execute;
    }
}
