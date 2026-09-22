/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;
import net.hasor.dataway.dal.jdbc.JdbcCallback;
import net.hasor.dataway.dal.jdbc.JdbcExecutor;

/** Resolves the host's transaction component after Hasor finishes constructing its container. */
public class LazyJdbcExecutor implements JdbcExecutor {
    private final Supplier<JdbcExecutor> provider;
    private       JdbcExecutor           delegate;

    public LazyJdbcExecutor(Supplier<JdbcExecutor> provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    private synchronized JdbcExecutor context() {
        if (delegate == null) {
            delegate = Objects.requireNonNull(provider.get());
        }
        return delegate;
    }

    @Override
    public <T> T execute(JdbcCallback<T> callback) throws SQLException {
        return context().execute(callback);
    }
}
