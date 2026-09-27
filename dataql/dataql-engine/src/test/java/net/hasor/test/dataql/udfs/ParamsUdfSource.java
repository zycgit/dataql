/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.test.dataql.udfs;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.domain.UdfParams;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataql.host.function.UdfName;

public class ParamsUdfSource extends AbstractUdfSource {
    @Override
    public Predicate<Method> getPredicate(Class<?> targetType) {
        return method -> method.isAnnotationPresent(UdfName.class);
    }

    @UdfName("leading")
    public static String leading(UdfParams params, Hints hints, String first, String second) {
        return first + ":" + second + ":" + hints.getHint("tag") + ":" + params.allParams().length;
    }

    @UdfName("middle")
    public String middle(String first, UdfParams params, Hints hints, String second) {
        return first + ":" + second + ":" + hints.getHint("tag") + ":" + params.allParams().length;
    }

    @UdfName("trailing")
    public String trailing(String first, String second, Hints hints, UdfParams params) {
        return first + ":" + second + ":" + hints.getHint("tag") + ":" + params.allParams().length;
    }

    @UdfName("same")
    public boolean same(UdfParams first, String value, UdfParams second) {
        return first == second && value.equals(first.allParams()[0]);
    }

    @UdfName("capture")
    public UdfParams capture(UdfParams params) {
        return params;
    }

    @UdfName("compare")
    public int compare(UdfParams params, int first, int second) {
        return Integer.compare(first, second);
    }
}
