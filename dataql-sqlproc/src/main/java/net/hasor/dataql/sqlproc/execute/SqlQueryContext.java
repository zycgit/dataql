/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.execute;
import java.util.Objects;
import net.hasor.cobble.ClassUtils;
import net.hasor.dataql.sqlproc.dynamic.DynamicSql;
import net.hasor.dataql.sqlproc.dynamic.MacroRegistry;
import net.hasor.dataql.sqlproc.dynamic.QueryContext;
import net.hasor.dataql.sqlproc.dynamic.rule.RuleRegistry;
import net.hasor.dataql.sqlproc.dynamic.rule.SqlRule;
import net.hasor.dataql.sqlproc.types.TypeHandlerRegistry;

public class SqlQueryContext implements QueryContext {
    private TypeHandlerRegistry typeRegistry  = TypeHandlerRegistry.DEFAULT;
    private MacroRegistry       macroRegistry = new MacroRegistry();
    private RuleRegistry        ruleRegistry  = new RuleRegistry();
    private ClassLoader         classLoader   = SqlQueryContext.class.getClassLoader();

    public MacroRegistry getMacroRegistry() {
        return this.macroRegistry;
    }

    public void setMacroRegistry(MacroRegistry macroRegistry) {
        this.macroRegistry = Objects.requireNonNull(macroRegistry, "macroRegistry is null.");
    }

    public void setTypeRegistry(TypeHandlerRegistry typeRegistry) {
        this.typeRegistry = Objects.requireNonNull(typeRegistry, "typeRegistry is null.");
    }

    public RuleRegistry getRuleRegistry() {
        return this.ruleRegistry;
    }

    public void setRuleRegistry(RuleRegistry ruleRegistry) {
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry is null.");
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader is null.");
    }

    @Override
    public SqlRule findRule(String ruleName) {
        return this.ruleRegistry.findRule(ruleName);
    }

    @Override
    public DynamicSql findMacro(String dynamicId) {
        return this.macroRegistry.findMacro(dynamicId);
    }

    @Override
    public TypeHandlerRegistry getTypeRegistry() {
        return this.typeRegistry;
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return ClassUtils.getClass(this.classLoader, className);
    }

    public void addMacro(String name, String segment) {
        this.macroRegistry.register(name, segment);
    }
}
