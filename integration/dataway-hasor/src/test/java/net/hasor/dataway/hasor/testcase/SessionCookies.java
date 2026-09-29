/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor.testcase;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/** Keeps separate browser sessions for each HTTP client. */
final class SessionCookies implements CookieJar {
    private final List<Cookie> cookies = new ArrayList<>();

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> received) {
        for (Cookie cookie : received) {
            this.cookies.removeIf(existing -> existing.name().equals(cookie.name()) && existing.path().equals(cookie.path()) && existing.domain().equals(cookie.domain()));
            if (cookie.expiresAt() > System.currentTimeMillis()) {
                this.cookies.add(cookie);
            }
        }
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        this.cookies.removeIf(cookie -> cookie.expiresAt() <= System.currentTimeMillis());
        return this.cookies.stream().filter(cookie -> cookie.matches(url)).toList();
    }
}
