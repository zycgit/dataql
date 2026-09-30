/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example.config.auth;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.SerializationException;
import io.jsonwebtoken.io.Serializer;
import net.hasor.dataql.util.JsonUtils;

/** Bridges JJWT to the repository's shared Jackson configuration. */
public class JwtJsonCodec implements Serializer<Map<String, ?>>, Deserializer<Map<String, ?>> {
    @Override
    public byte[] serialize(Map<String, ?> value) {
        try {
            return JsonUtils.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException failure) {
            throw new SerializationException("Cannot serialize JWT JSON", failure);
        }
    }

    @Override
    public void serialize(Map<String, ?> value, OutputStream output) {
        try {
            JsonUtils.writeValue(output, value);
        } catch (RuntimeException failure) {
            throw new SerializationException("Cannot serialize JWT JSON", failure);
        }
    }

    @Override
    public Map<String, ?> deserialize(byte[] value) {
        return this.deserialize(new InputStreamReader(new ByteArrayInputStream(value), StandardCharsets.UTF_8));
    }

    @Override
    public Map<String, ?> deserialize(Reader input) {
        try {
            return JsonUtils.readValue(input, Map.class);
        } catch (RuntimeException failure) {
            throw new DeserializationException("Cannot deserialize JWT JSON", failure);
        }
    }
}
