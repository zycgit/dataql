/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.hasor.cobble.ResourcesUtils;
import net.hasor.cobble.StringUtils;
import net.hasor.cobble.io.IOUtils;
import net.hasor.cobble.logging.Logger;
import net.hasor.cobble.logging.LoggerFactory;
import net.hasor.dataql.compiler.CompilerArguments;
import net.hasor.dataql.compiler.CompilerHelper;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.Query;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.kernel.Finder;
import net.hasor.dataql.parser.QueryModel;
import net.hasor.dataql.parser.ast.AstVisitor;
import net.hasor.dataql.parser.ast.InstVisitorContext;

/**
 * 测试用例
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-19
 */
public class AbstractTestResource {
    protected static Logger logger = LoggerFactory.getLogger(AbstractTestResource.class);

    protected String getScript(String queryResource) throws IOException {
        InputStream inStream = ResourcesUtils.getResourceAsStream(queryResource);
        if (inStream == null) {
            return "";
        }
        // .获取 DataQL 查询字符串
        logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        logger.info("resource = " + queryResource);
        InputStreamReader reader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
        StringWriter outWriter = new StringWriter();
        IOUtils.copy(reader, outWriter);
        String buildQuery = outWriter.toString();
        logger.info("\n" + buildQuery);
        logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        return buildQuery.replace("\r\n", "\n");
    }

    protected Query compilerQL(String qlString) throws IOException {
        QueryModel queryModel = CompilerHelper.queryParser(qlString);
        return new QueryManager(new HostConfiguration().getHostContext()).newBuilder().createQuery(queryModel, CompilerArguments.DEFAULT);
    }

    protected Query compilerQL(String qlString, Finder finder) throws IOException {
        QueryModel queryModel = CompilerHelper.queryParser(qlString);
        HostConfiguration configuration = new HostConfiguration(finder);
        return new QueryManager(configuration.getHostContext()).newBuilder().createQuery(queryModel, CompilerArguments.DEFAULT);
    }

    protected List<String> acceptVisitor(QueryModel queryModel) {
        List<String> astVisitor = new ArrayList<>();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        queryModel.accept(new AstVisitor() {
            @Override
            public void visitInst(InstVisitorContext inst) {
                String fixedString = StringUtils.repeat(' ', atomicInteger.get() * 4);
                String dataIn = "IN - " + inst.getInst().getClass().getSimpleName();
                String dataOut = "OUT - " + inst.getInst().getClass().getSimpleName();
                //
                astVisitor.add(fixedString + dataIn);
                atomicInteger.incrementAndGet();
                inst.visitChildren(this);
                atomicInteger.decrementAndGet();
                astVisitor.add(fixedString + dataOut);
            }
        });
        return astVisitor;
    }
}
