/*
 * Copyright 2008-2009 the original author or authors.
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
package net.hasor.dataql.spi;
import net.hasor.dataql.service.DefaultFinder;

/**
 * Finder 扩展注册接口。第三方扩展包可以通过 {@link java.util.ServiceLoader}
 * 发布该接口实现，将自身提供的 import 资源、FragmentProcess 等能力注册到默认 Finder 中。
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2026-07-10
 */
public interface FinderProvider {
    /** 将扩展能力注册到 Finder。 */
    void loadTo(DefaultFinder finder);
}
