package net.hasor.dataql.sqlproc.execute.support;

import java.util.List;
import net.hasor.dataql.sqlproc.dynamic.config.SqlConfig;
import net.hasor.dataql.sqlproc.execute.interceptor.SqlExecutionInterceptor;

record FragmentConfig(SqlConfig config, List<SqlExecutionInterceptor> interceptors) {
}
