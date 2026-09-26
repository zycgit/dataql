/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.host;
import net.hasor.dataql.kernel.Finder;

/**
 * Host 上下文，为 SPI 扩展点（FragmentProcessFactory、UdfSourceFactory）提供宿主环境信息。
 * 继承 {@link Finder}，SPI 工厂可直接通过上下文进行资源查找。
 */
public interface HostContext extends Finder {

    /** Return the host-scoped attachment, creating it through SPI when first requested. */
    <T> T getAttachment(Class<T> attachmentType);

    /** Add a host-scoped attachment created while initializing another attachment. */
    <T> void addAttachment(Class<T> attachmentType, T attachment);
}
