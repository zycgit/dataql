/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql.sqlproc.dynamic.segment;

public class SqlModifier {
    /**
     * the position arg.
     */
    public static final int POSITION  = 1;
    /**
     * the named arg.
     */
    public static final int NAMED     = 2;
    /**
     * the sql injection.
     */
    public static final int INJECTION = 4;
    /**
     * include rule
     */
    public static final int RULE      = 8;

    public static boolean hasPosition(int modifier) {
        return (POSITION & modifier) != 0;
    }

    public static boolean hasNamed(int modifier) {
        return (NAMED & modifier) != 0;
    }

    public static boolean hasInjection(int modifier) {
        return (INJECTION & modifier) != 0;
    }

    public static boolean hasRule(int modifier) {
        return (RULE & modifier) != 0;
    }
}
