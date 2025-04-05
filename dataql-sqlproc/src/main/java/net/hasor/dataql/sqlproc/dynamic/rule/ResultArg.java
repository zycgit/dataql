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
package net.hasor.dataql.sqlproc.dynamic.rule;

@Deprecated
public class ResultArg {
    public static final String        CFG_KEY_NAME = ArgRule.CFG_KEY_NAME;
    private             ResultArgType argType;
    private             String        name;

    public ResultArg(String name, ResultArgType argType) {
        this.name = name;
        this.argType = argType;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResultArgType getArgType() {
        return this.argType;
    }

    public void setArgType(ResultArgType argType) {
        this.argType = argType;
    }
}
