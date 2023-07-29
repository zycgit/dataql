package net.hasor.dataql.sqlproc.dynamic;

import net.hasor.dataql.sqlproc.repository.DynamicContext;
import net.hasor.dataql.sqlproc.repository.DynamicSql;
import net.hasor.dataql.sqlproc.repository.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.repository.rule.SqlBuildRule;
import net.hasor.dataql.sqlproc.types.TypeHandler;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

public class TextBuilderContext implements DynamicContext {
    private TypeHandlerRegistry handlerRegistry = new TypeHandlerRegistry();
    private RuleRegistry        ruleRegistry    = new RuleRegistry();
    private ClassLoader         classLoader     = Thread.currentThread().getContextClassLoader();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.classLoader.loadClass(name);
    }

    @Override
    public DynamicSql findDynamic(String dynamicId) {
        return null;
    }

    @Override
    public TypeHandler<?> findTypeHandler(Class<?> handlerType) {
        return this.handlerRegistry.getTypeHandler(handlerType);
    }

    @Override
    public TypeHandler<?> findTypeHandler(Class<?> javaType, Integer jdbcType) {
        return this.handlerRegistry.getTypeHandler(javaType, jdbcType);
    }

    @Override
    public TypeHandler<?> defaultTypeHandler() {
        return this.handlerRegistry.getDefaultTypeHandler();
    }

    @Override
    public SqlBuildRule findRule(String ruleName) {
        return this.ruleRegistry.findByName(ruleName);
    }
}
