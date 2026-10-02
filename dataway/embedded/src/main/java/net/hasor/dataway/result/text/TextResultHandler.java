/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result.text;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataql.domain.BinaryModel;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;

/** Writes the string representation of a script value as UTF-8 text. */
public class TextResultHandler extends AbstractResultHandler {
    public TextResultHandler() {
        this(Map.of());
    }

    public TextResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        if (!context.isSuccess()) {
            return new StructureResultHandler().handle(context);
        }

        Object value = context.getValue();
        if (value instanceof BinaryModel || value instanceof InputStream) {
            throw new IllegalArgumentException("Use Raw Value to return binary content");
        }

        String text = String.valueOf(value);
        return ResultInfoUtils.convertToResultInfo("text/plain; charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
    }
}
