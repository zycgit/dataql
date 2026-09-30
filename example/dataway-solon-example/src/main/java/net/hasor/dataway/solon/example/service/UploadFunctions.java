/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.service;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import net.hasor.dataql.host.function.AbstractUdfSource;
import net.hasor.dataway.model.WebFile;

/** Reads uploads during API execution; Dataway releases their temporary storage afterwards. */
public class UploadFunctions extends AbstractUdfSource {
    public Map<String, Object> inspect(WebFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        try (var input = file.openStream()) {
            int size;
            while ((size = input.read(buffer)) != -1) {
                digest.update(buffer, 0, size);
            }
        }
        return Map.of("name", file.getName(), "size", file.getSize(),
                "contentType", file.getContentType(), "sha256", HexFormat.of().formatHex(digest.digest()));
    }
}
