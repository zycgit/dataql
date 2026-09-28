/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.hasor.cobble.StringUtils;

/** Buffers script header changes until the host response is opened. */
public abstract class WebResponse {
    private       boolean            started;
    private       boolean            head;
    private final List<HeaderChange> changes = new ArrayList<>();

    protected abstract OutputStream openBody(int status) throws IOException;

    /** Applies one header operation without committing the response. */
    protected abstract void writeHeader(String name, String value, boolean append);

    public abstract boolean isCommitted();

    public final void prepare(WebRequest request) {
        this.head = StringUtils.equalsIgnoreCase(request.getMethod(), "HEAD");
    }

    public final boolean isStarted() {
        return this.started;
    }

    public final void setHeader(String name, String value) {
        this.changeHeader(name, value, false);
    }

    public final void addHeader(String name, String value) {
        this.changeHeader(name, value, true);
    }

    public final void setCookie(WebCookie cookie) {
        this.addHeader("Set-Cookie", cookie.toHeaderValue());
    }

    private void changeHeader(String name, String value, boolean append) {
        this.requireOpen();
        validateHeader(name, value);

        this.changes.add(new HeaderChange(name, value, append));
    }

    private void requireOpen() {
        if (this.started || this.isCommitted()) {
            throw new IllegalStateException("Response already started");
        }
    }

    static void validateHeader(String name, String value) {
        if (name == null || !name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
            throw new IllegalArgumentException("Invalid header name");
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == 127 || ch < 32 && ch != '\t') {
                throw new IllegalArgumentException("Invalid header value");
            }
        }
    }

    public final OutputStream write(int status, Map<String, String> headers) throws IOException {
        this.requireOpen();
        headers.forEach(WebResponse::validateHeader);
        this.started = true;

        headers.forEach((name, value) -> this.writeHeader(name, value, StringUtils.equalsIgnoreCase(name, "Set-Cookie")));
        for (HeaderChange change : this.changes) {
            this.writeHeader(change.name(), change.value(), change.append());
        }

        OutputStream output = this.openBody(status);
        return this.head ? OutputStream.nullOutputStream() : output;
    }

    private record HeaderChange(String name, String value, boolean append) {
    }
}
