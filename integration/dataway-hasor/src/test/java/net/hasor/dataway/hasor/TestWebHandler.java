/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.List;
import java.util.function.BiFunction;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.model.WebResponse;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.service.WebHandler;

/** Custom entry used to exercise the public handler extension contract. */
public class TestWebHandler extends WebHandler {
    private final List<String>                                    paths;
    private final BiFunction<WebRequest, WebResponse, ResultInfo> result;

    public TestWebHandler(Dataway dataway, List<String> paths, BiFunction<WebRequest, WebResponse, ResultInfo> result) {
        super(dataway);
        this.paths = List.copyOf(paths);
        this.result = result;
    }

    @Override
    public List<String> paths() {
        return this.paths;
    }

    @Override
    protected ResultInfo handleRequest(WebRequest request, WebResponse response) {
        return this.result.apply(request, response);
    }
}
