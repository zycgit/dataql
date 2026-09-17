/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataql;
import net.hasor.dataql.parser.location.Location;

/**
 * DataQL 异常
 * @author 赵永春 (zyc@hasor.net)
 * @version : 2017-07-14
 */
public class DataQueryException extends RuntimeException {
    protected final Location location;
    private final   String   localizedMessage;

    public DataQueryException(Location location, String errorMessage) {
        super(errorMessage(location, errorMessage));
        this.location = location;
        this.localizedMessage = errorMessage;
    }

    public DataQueryException(Location location, String errorMessage, Throwable e) {
        super(errorMessage(location, errorMessage), e);
        this.location = location;
        this.localizedMessage = errorMessage;
    }

    public DataQueryException(Location location, Throwable e) {
        super(errorMessage(location, e.getLocalizedMessage()), e);
        this.location = location;
        this.localizedMessage = e.getLocalizedMessage();
    }

    private static String errorMessage(Location location, String errorMessage) {
        return "[" + location.toString() + "] " + errorMessage;
    }

    public String getLocalizedMessage() {
        return this.localizedMessage;
    }

    public final Location getLocation() {
        return this.location;
    }
}
