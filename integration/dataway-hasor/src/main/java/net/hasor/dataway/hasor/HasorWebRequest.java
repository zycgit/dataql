/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.web.body.BodyReaders;
import net.hasor.dataway.web.body.FormBodyReader;
import net.hasor.web.FileItemStream;
import net.hasor.web.upload.FileUpload;

/** Delegates body access to the request passed through the host's MVC chain. */
public class HasorWebRequest extends WebRequest {
    private final HttpServletRequest request;
    private       FileUpload         fileUpload = new FileUpload();

    public HasorWebRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setFileUpload(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }

    @Override
    public Map<String, Object> getFormBody() {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        this.request.getParameterMap().forEach((name, values) -> parameters.put(name, Arrays.asList(values)));

        Map<String, Object> body = FormBodyReader.fromParameters(parameters, this.getQuery());
        if (body.isEmpty() && !StringUtils.equalsIgnoreCase(this.getMethod(), "POST")) {
            return null;
        }

        return body;
    }

    @Override
    public Map<String, Object> getMultipartBody(Charset charset) throws IOException {
        this.fileUpload.setHeaderEncoding(charset.name());
        Iterator<FileItemStream> items = this.fileUpload.getItemIterator(this.request);
        Map<String, Object> body = new LinkedHashMap<>();

        while (items.hasNext()) {
            FileItemStream item = items.next();
            try (InputStream input = item.openStream()) {
                Object value;
                if (item.isFormField()) {
                    Charset encoding = BodyReaders.charset(item.getContentType(), charset);
                    value = new String(input.readAllBytes(), encoding);
                } else {
                    value = this.cacheFile(item.getName(), item.getContentType(), input);
                }
                FormBodyReader.add(body, item.getFieldName(), value);
            }
        }

        return body;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.request.getInputStream();
    }

    @Override
    public Object getAttribute(String name) {
        return this.request.getAttribute(name);
    }
}
