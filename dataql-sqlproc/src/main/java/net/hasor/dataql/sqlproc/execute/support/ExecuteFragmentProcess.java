package net.hasor.dataql.sqlproc.execute.support;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.io.IOUtils;
import net.hasor.dataql.domain.HintNames;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsProxy;
import net.hasor.dataql.sqlproc.ConfigFormatType;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.config.QueryType;

/**
 * SQL FragmentProcess 实现。通过 {@link #inferQueryType(String)} 自动推断查询类型，
 * 同时支持 @@type 前缀显式覆盖。
 */
public class ExecuteFragmentProcess extends AbstractSqlFragment {
    public ExecuteFragmentProcess(QueryContext queryContext) {
        super(queryContext);
    }

    @Override
    protected QueryType queryType(String fragmentString, Hints hints) {
        FragmentBody fragmentBody = parseFragmentBody(fragmentString);
        return fragmentBody.queryType != null ? fragmentBody.queryType : inferQueryType(fragmentString);
    }

    @Override
    protected FragmentConfig buildConfig(String fragmentString, Hints hints) {
        FragmentBody fragmentBody = parseFragmentBody(fragmentString);
        if (fragmentBody.queryType != null) {
            return super.buildConfig(fragmentBody.fragmentString, typeHints(hints, fragmentBody.queryType));
        }

        Object hintValue = hints.getHint(HintNames.FRAGMENT_TYPE.name());
        if (hintValue != null && StringUtils.isNotBlank(hintValue.toString())) {
            return super.buildConfig(fragmentString, hints);
        }

        return super.buildConfig(fragmentString, typeHints(hints, inferQueryType(fragmentString)));
    }

    // ----------------------------------------------------------------
    // Type / explicit type
    // ----------------------------------------------------------------

    public static QueryType parseExplicitType(String fragmentString) {
        return parseFragmentBody(fragmentString).queryType;
    }

    private static FragmentBody parseFragmentBody(String fragmentString) {
        if (fragmentString == null) {
            return new FragmentBody(null, null);
        }

        String fragmentBody = fragmentString.stripLeading();
        if (!fragmentBody.startsWith("@@")) {
            return new FragmentBody(null, fragmentString);
        }

        int idx = 2;
        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < fragmentBody.length() && Character.isJavaIdentifierPart(fragmentBody.charAt(i)); i++) {
            sb.append(fragmentBody.charAt(i));
        }

        QueryType queryType = sb.isEmpty() ? null : QueryType.valueOfTag(sb.toString());
        if (queryType == null) {
            return new FragmentBody(null, fragmentString);
        }

        String sqlBody = fragmentBody.substring(idx + sb.length()).stripLeading();
        return new FragmentBody(queryType, sqlBody);
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

            return valueOfSql(line);
        }
        return QueryType.Execute;
    }

    private Hints typeHints(Hints hints, QueryType queryType) {
        Object hintValue = hints.getHint(HintNames.FRAGMENT_TYPE.name());
        String fragmentType = hintValue == null ? null : hintValue.toString();
        ConfigFormatType formatType = this.resolveFormatType(fragmentType);
        String proxyFragmentType = queryType.getTagString() + (formatType == ConfigFormatType.Xml ? "Xml" : "Sql");
        return new HintsProxy(hints) {
            @Override
            public Object getHint(String optionKey) {
                if (HintNames.FRAGMENT_TYPE.name().equals(optionKey)) {
                    return proxyFragmentType;
                }
                return super.getHint(optionKey);
            }
        };
    }

    private static QueryType valueOfSql(String sqlString) {
        String lower = sqlString.toLowerCase();
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

    private record FragmentBody(QueryType queryType, String fragmentString) {
    }
}
