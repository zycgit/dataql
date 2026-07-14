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
