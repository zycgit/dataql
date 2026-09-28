/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.web.body.FormBodyReader;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

public class SolonWebRequest extends WebRequest {
    private final Context context;

    public SolonWebRequest(Context context) {
        this.context = context;
    }

    @Override
    public Map<String, Object> getFormBody() {
        return FormBodyReader.fromParameters(this.context.paramMap().toValuesMap(), this.getQuery());
    }

    @Override
    public Map<String, Object> getMultipartBody(Charset charset) throws IOException {
        Map<String, List<UploadedFile>> files = this.context.fileMap().toValuesMap();
        for (List<UploadedFile> entries : files.values()) {
            for (UploadedFile file : entries) {
                this.registerResource(file::delete);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, List<UploadedFile>> entry : files.entrySet()) {
            for (UploadedFile file : entry.getValue()) {
                try (InputStream input = file.getContent()) {
                    Object cached = this.cacheFile(file.getName(), file.getContentType(), input);
                    FormBodyReader.add(body, entry.getKey(), cached);
                }
            }
        }

        this.getFormBody().forEach((name, value) -> {
            if (value instanceof List<?> values) {
                values.forEach(item -> FormBodyReader.add(body, name, item));
            } else {
                FormBodyReader.add(body, name, value);
            }
        });

        return body;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.context.bodyAsStream();
    }

    @Override
    public Object getAttribute(String name) {
        return this.context.attr(name);
    }
}
