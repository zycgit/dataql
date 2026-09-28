/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import net.hasor.cobble.StringUtils;
import net.hasor.dataway.model.WebRequest;
import net.hasor.dataway.web.body.FormBodyReader;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.util.WebUtils;

/** Delegates body access to the request passed through the host's MVC chain. */
public class SpringWebRequest extends WebRequest {
    private final HttpServletRequest          request;
    private       MultipartHttpServletRequest localMultipart;

    public SpringWebRequest(HttpServletRequest request) {
        this.request = request;
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
        MultipartHttpServletRequest multipart = WebUtils.getNativeRequest(this.request, MultipartHttpServletRequest.class);
        if (multipart == null) {
            this.localMultipart = new StandardMultipartHttpServletRequest(this.request, true);
            multipart = this.localMultipart;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, List<MultipartFile>> entry : multipart.getMultiFileMap().entrySet()) {
            for (MultipartFile file : entry.getValue()) {
                try (InputStream input = file.getInputStream()) {
                    Object cached = this.cacheFile(file.getOriginalFilename(), file.getContentType(), input);
                    FormBodyReader.add(body, entry.getKey(), cached);
                }
            }
        }

        Map<String, List<String>> parameters = new LinkedHashMap<>();
        multipart.getParameterMap().forEach((name, values) -> parameters.put(name, Arrays.asList(values)));
        Map<String, Object> fields = FormBodyReader.fromParameters(parameters, this.getQuery());
        fields.forEach((name, value) -> {
            if (value instanceof List<?> values) {
                values.forEach(item -> FormBodyReader.add(body, name, item));
            } else {
                FormBodyReader.add(body, name, value);
            }
        });
        return body;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (this.localMultipart != null) {
                new StandardServletMultipartResolver().cleanupMultipart(this.localMultipart);
                this.localMultipart = null;
            }
        }
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
