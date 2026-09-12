/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
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
