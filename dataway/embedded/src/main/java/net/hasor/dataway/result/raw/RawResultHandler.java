/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result.raw;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;

/** Returns the original value, retaining error details when a failure has no value. */
public class RawResultHandler extends AbstractResultHandler {
    public RawResultHandler() {
        this(Map.of());
    }

    public RawResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        if (!context.isSuccess() && context.getValue() == null) {
            return new StructureResultHandler().handle(context);
        }
        return ResultInfoUtils.toResult(context.getValue());
    }
}
