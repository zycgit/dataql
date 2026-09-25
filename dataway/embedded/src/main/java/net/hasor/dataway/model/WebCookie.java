/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.model;
import java.util.Locale;
import java.util.Objects;

/** Framework-neutral response cookie; values are supplied in their wire representation. */
public class WebCookie {
    private String  name;
    private String  value;
    private String  path = "/";
    private String  domain;
    private Long    maxAge;
    private boolean secure;
    private boolean httpOnly;
    private String  sameSite;

    public WebCookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getMaxAge() {
        return this.maxAge;
    }

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isSecure() {
        return this.secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public String getSameSite() {
        return this.sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String toHeaderValue() {
        WebResponse.validateHeader(this.name, Objects.requireNonNull(this.value, "cookie value"));
        for (int i = 0; i < this.value.length(); i++) {
            char ch = this.value.charAt(i);
            if (ch < 33 || ch > 126 || ch == '"' || ch == ',' || ch == ';' || ch == '\\') {
                throw new IllegalArgumentException("Cookie values must be encoded before writing");
            }
        }
        StringBuilder header = new StringBuilder(this.name).append('=').append(this.value);
        this.attribute(header, "Path", this.path);
        this.attribute(header, "Domain", this.domain);
        if (this.maxAge != null && this.maxAge >= 0) {
            header.append("; Max-Age=").append(this.maxAge);
            if (this.maxAge == 0) {
                header.append("; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            }
        }
        if (this.secure) {
            header.append("; Secure");
        }
        if (this.httpOnly) {
            header.append("; HttpOnly");
        }
        if (this.sameSite != null) {
            String policy = switch (this.sameSite.toLowerCase(Locale.ROOT)) {
                case "lax" -> "Lax";
                case "strict" -> "Strict";
                case "none" -> "None";
                default -> {
                    throw new IllegalArgumentException("Invalid SameSite policy");
                }
            };
            if (policy.equals("None") && !this.secure) {
                throw new IllegalArgumentException("SameSite=None requires Secure");
            }
            header.append("; SameSite=").append(policy);
        }
        return header.toString();
    }

    private void attribute(StringBuilder header, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 32 || ch > 126 || ch == ';') {
                throw new IllegalArgumentException("Invalid cookie " + name);
            }
        }
        header.append("; ").append(name).append('=').append(value);
    }
}
