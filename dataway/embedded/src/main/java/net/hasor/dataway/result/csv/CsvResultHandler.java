/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.result.csv;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.ResultInfo;
import net.hasor.dataway.result.AbstractResultHandler;
import net.hasor.dataway.result.ResultContext;
import net.hasor.dataway.result.structure.StructureResultHandler;
import net.hasor.dataway.service.ResultInfoUtils;

/** Writes a list of rows as UTF-8 CSV, retaining column order and escaping CSV delimiters. */
public class CsvResultHandler extends AbstractResultHandler {
    public CsvResultHandler() {
        this(Map.of());
    }

    public CsvResultHandler(Map<String, ?> defaults) {
        super(defaults);
    }

    @Override
    public ResultInfo handle(ResultContext context) {
        if (!context.isSuccess()) {
            return new StructureResultHandler().handle(context);
        }

        Object value = context.getValue();
        if (!(value instanceof List<?> rows)) {
            throw new IllegalArgumentException("CSV result must be a list of objects");
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> fields)) {
                throw new IllegalArgumentException("CSV rows must be objects");
            }

            for (Object name : fields.keySet()) {
                if (!(name instanceof String column)) {
                    throw new IllegalArgumentException("CSV column names must be strings");
                }
                names.add(column);
            }
        }

        List<String> columns = new ArrayList<>(names);
        StringBuilder csv = new StringBuilder();
        if (!columns.isEmpty()) {
            this.appendRow(csv, columns);
            for (Object row : rows) {
                Map<?, ?> fields = (Map<?, ?>) row;
                List<Object> cells = new ArrayList<>();
                for (String column : columns) {
                    cells.add(fields.get(column));
                }
                this.appendRow(csv, cells);
            }
        }

        ResultInfo response = ResultInfoUtils.binary("text/csv; charset=UTF-8", csv.toString().getBytes(StandardCharsets.UTF_8));
        response.getHeaders().put("Content-Disposition", "attachment; filename=results.csv");
        return response;
    }

    private void appendRow(StringBuilder csv, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }

            Object value = cells.get(i);
            if (value != null && !(value instanceof CharSequence) && !(value instanceof Number) && !(value instanceof Boolean)) {
                throw new IllegalArgumentException("CSV cells must be scalar values");
            }

            String text = value == null ? "" : value.toString();
            if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
                csv.append('"').append(text.replace("\"", "\"\"")).append('"');
            } else {
                csv.append(text);
            }
        }
        csv.append("\r\n");
    }
}
