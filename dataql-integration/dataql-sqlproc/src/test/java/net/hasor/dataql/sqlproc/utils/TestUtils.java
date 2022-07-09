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
package net.hasor.dataql.sqlproc.utils;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @version : 2013-12-10
 * @author 赵永春 (zyc@hasor.net)
 */
public class TestUtils {
    public static final  String   INSERT_ARRAY = "insert into tb_user (userUUID,name,loginName,loginPassword,email,`index`,registerTime) values (?,?,?,?,?,?,?);";
    private static final Object[] DATA_1       = new Object[] { newID(), "默罕默德", "muhammad", "1", "muhammad@hasor.net", 1, new Date() };
    private static final Object[] DATA_2       = new Object[] { newID(), "安妮.贝隆", "belon", "2", "belon@hasor.net", 2, new Date() };
    private static final Object[] DATA_3       = new Object[] { newID(), "赵飞燕", "feiyan", "3", "feiyan@hasor.net", 3, new Date() };

    public static String newID() {
        return UUID.randomUUID().toString();
    }

    public static Object[] arrayForData1() {
        return DATA_1;
    }

    public static Object[] arrayForData2() {
        return DATA_2;
    }

    public static Object[] arrayForData3() {
        return DATA_3;
    }

}