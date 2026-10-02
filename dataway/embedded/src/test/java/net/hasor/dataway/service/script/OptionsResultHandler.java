/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.script;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.service.ResultInfoUtils;

/** An application handler whose defaults and API options control its response. */
class OptionsResultHandler extends AbstractResultHandler {
    OptionsResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        Map<String, Object> options = context.getOptions();
        int status = ((Number) options.get("status")).intValue();
        List<?> labels = (List<?>) options.get("labels");
        List<?> responseLabels = new ArrayList<>(labels);
        labels.clear();
        return ResultInfoUtils.json(status, Map.of("labels", responseLabels, "value", context.getValue()));
    }
}
