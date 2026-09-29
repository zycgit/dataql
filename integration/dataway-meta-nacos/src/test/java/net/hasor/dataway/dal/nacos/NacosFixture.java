/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.nacos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import com.alibaba.nacos.api.config.ConfigService;
import net.hasor.dataway.dal.FieldDef;
import static net.hasor.dataway.dal.FieldDef.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Models Nacos atomic compare-and-set while keeping all transport in a mock client. */
final class NacosFixture {
    final ConfigService        client    = mock(ConfigService.class);
    final Map<String, String>  documents = new ConcurrentHashMap<>();
    final NacosDataAccessLayer access;

    NacosFixture() throws Exception {
        this.documents.put("group/metadata", NacosSnapshot.empty().serialize());
        when(this.client.getConfig(anyString(), anyString(), anyLong())).thenAnswer(call -> this.documents.get(call.getArgument(1) + "/" + call.getArgument(0)));
        when(this.client.publishConfigCas(anyString(), anyString(), anyString(), anyString())).thenAnswer(call -> {
            String key = call.getArgument(1) + "/" + call.getArgument(0);
            String content = call.getArgument(2);
            String digest = call.getArgument(3);
            AtomicBoolean updated = new AtomicBoolean();
            this.documents.compute(key, (ignored, previous) -> {
                if (previous != null && digest.equals(NacosFixture.md5(previous))) {
                    updated.set(true);
                    return content;
                }
                return previous;
            });
            return updated.get();
        });
        this.access = this.reconnect();
    }

    NacosDataAccessLayer reconnect() {
        return new NacosDataAccessLayer(this.client, "metadata", "group", 2000);
    }

    String content() {
        return this.documents.get("group/metadata");
    }

    void content(String content) {
        if (content == null) {
            this.documents.remove("group/metadata");
        } else {
            this.documents.put("group/metadata", content);
        }
    }

    static String md5(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    static Map<FieldDef, String> route(String method, String path) {
        return Map.of(METHOD, method, PATH, path, SCRIPT, "return '中文';", TYPE, "DataQL", COMMENT, "metadata");
    }
}
