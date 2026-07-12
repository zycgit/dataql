package net.hasor.dataql.sqlproc.execute.fragment;

import java.util.List;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;

public record FragmentConfig(SqlConfig config, List<SqlExecutionInterceptor> interceptors) {
}
