/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.dataql.beans;
import java.util.ArrayList;
import java.util.List;
import net.hasor.dataql.host.function.AbstractUdfSource;

/**
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2019-12-12
 */
public class UserOrderUdfSource extends AbstractUdfSource {
    /** user_list */
    public static List<UserBean> userList() {
        return new ArrayList<UserBean>() {{
            add(new UserBean(1));
            add(new UserBean(2));
            add(new UserBean(3));
            add(new UserBean(4));
        }};
    }

    /** order_list */
    public static List<OrderBean> orderList(final long accountID) {
        return new ArrayList<OrderBean>() {{
            add(new OrderBean(accountID, 1));
            add(new OrderBean(accountID, 2));
            add(new OrderBean(accountID, 3));
            add(new OrderBean(accountID, 4));
        }};
    }
}
