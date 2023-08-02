package net.hasor.dataql.sqlproc.repository;

import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.DynamicSql;
import net.hasor.dataql.sqlproc.repository.nodes.TextDynamicSql;
import net.hasor.dataql.sqlproc.repository.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.repository.rule.SqlBuildRule;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

public class TextBuilderContext implements DynamicContext {
    private final TypeHandlerRegistry handlerRegistry = new TypeHandlerRegistry();
    private final RuleRegistry        ruleRegistry    = new RuleRegistry();
    private final ClassLoader         classLoader     = Thread.currentThread().getContextClassLoader();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.classLoader.loadClass(name);
    }

    @Override
    public DynamicSql findDynamic(String dynamicId) {
        return new TextDynamicSql("<include form " + dynamicId + ">");
    }

    @Override
    public TypeHandler<?> findJavaTypeHandler(Class<?> javaType, int jdbcType) {
        return this.handlerRegistry.getJavaTypeHandler(javaType, jdbcType);
    }

    @Override
    public SqlBuildRule findRule(String ruleName) {
        return this.ruleRegistry.findByName(ruleName);
    }
}
