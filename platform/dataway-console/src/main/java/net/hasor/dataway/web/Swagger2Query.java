package net.hasor.dataway.web;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryBuilder;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.CustomizeScope;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.kernel.QueryResult;
import net.hasor.dataql.kernel.QueryRuntimeException;
import net.hasor.dataql.parser.QueryParseException;
import net.hasor.utils.ResourcesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
// Generated from '/META-INF/hasor-framework/dataway-swagger2.ql'

public class Swagger2Query extends HintsSet implements Query {
    protected final String sourceCode = "/META-INF/hasor-framework/dataway-swagger2.ql";
    protected       Query  dataQuery;

    private Swagger2Query(HintsSet hintsSet) {
        this.setHints(hintsSet);
    }

    public Swagger2Query() throws IOException, QueryParseException {
        this(new HostConfiguration(), Collections.emptyMap());
    }

    public Swagger2Query(Finder finder, Map<String, Supplier<?>> shareVarMap) throws IOException, QueryParseException {
        InputStream inputStream = Objects.requireNonNull(ResourcesUtils.getResourceAsStream(sourceCode), sourceCode);
        HostConfiguration configuration = new HostConfiguration(finder);
        QueryBuilder queryBuilder = new QueryManager(configuration.getHostContext()).newBuilder();
        shareVarMap.forEach(queryBuilder::addShareVar);
        this.dataQuery = queryBuilder.createQuery(inputStream, StandardCharsets.UTF_8);
    }

    @Override
    public void addShareVar(String key, Object value) {
        this.dataQuery.addShareVar(key, value);
    }

    @Override
    public QueryResult execute(CustomizeScope customizeScope) throws QueryRuntimeException {
        this.dataQuery.setHints(this);
        return this.dataQuery.execute(customizeScope);
    }

    @Override
    public Swagger2Query clone() {
        Swagger2Query clone = new Swagger2Query(this);
        clone.dataQuery = this.dataQuery.clone();
        return clone;
    }
}
