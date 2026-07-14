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
package net.hasor.dataql.host.jsr223;

import java.util.Objects;
import java.util.function.Supplier;
import javax.script.SimpleScriptContext;
import net.hasor.cobble.loader.ResourceLoader;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.HintsSet;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.HostContext;
import net.hasor.dataql.kernel.FragmentProcess;

/**
 * JSR223 脚本上下文，同时作为 DataQL Host 上下文。
 */
public class QueryScriptContext extends SimpleScriptContext implements HostContext, Hints {
    private final HostContext context;
    private final HintsSet    hints = new HintsSet();

    public QueryScriptContext(HostContext context) {
        this.context = Objects.requireNonNull(context, "hostContext is null.");
    }

    public HostContext getHostContext() {
        return this.context;
    }

    public void addFragment(String name, Supplier<? extends FragmentProcess> provider) {
        this.getConfiguration().addFragment(name, provider);
    }

    public void addImport(String name, Supplier<?> provider) {
        this.getConfiguration().addImport(name, provider);
    }

    public HostConfiguration getConfiguration() {
        if (this.context instanceof HostConfiguration) {
            return (HostConfiguration) this.context;
        }
        throw new UnsupportedOperationException("hostContext is not HostConfiguration.");
    }

    @Override
    public String[] getHints() {
        return this.hints.getHints();
    }

    @Override
    public Object getHint(String optionKey) {
        return this.hints.getHint(optionKey);
    }

    @Override
    public void removeHint(String optionKey) {
        this.hints.removeHint(optionKey);
    }

    @Override
    public void setHint(String hintName, String value) {
        this.hints.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, Number value) {
        this.hints.setHint(hintName, value);
    }

    @Override
    public void setHint(String hintName, boolean value) {
        this.hints.setHint(hintName, value);
    }

    @Override
    public <T> T getAttachment(Class<T> attachmentType) {
        return this.context.getAttachment(attachmentType);
    }

    @Override
    public <T> void addAttachment(Class<T> attachmentType, T attachment) {
        this.context.addAttachment(attachmentType, attachment);
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return this.context.getResourceLoader();
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.context.getClassLoader();
    }

    @Override
    public Object findBean(String beanName) throws ClassNotFoundException {
        return this.context.findBean(beanName);
    }

    @Override
    public Object findBean(Class<?> beanType) {
        return this.context.findBean(beanType);
    }

    @Override
    public FragmentProcess findFragmentProcess(String fragmentType) {
        return this.context.findFragmentProcess(fragmentType);
    }
}
