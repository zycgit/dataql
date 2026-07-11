/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * ...
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
