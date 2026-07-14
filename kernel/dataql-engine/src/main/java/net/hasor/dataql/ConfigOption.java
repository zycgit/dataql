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
package net.hasor.dataql;

/**
 * DataQL 编译配置项。
 */
public enum ConfigOption {
    /** 代码行号的编译模式 */
    CODE_LOCATION("codeLocation");

    private final String configName;

    public String getConfigName() {
        return this.configName;
    }

    ConfigOption(String configName) {
        this.configName = configName;
    }
}
