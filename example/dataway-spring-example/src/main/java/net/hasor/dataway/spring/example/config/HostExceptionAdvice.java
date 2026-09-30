/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.example.config;
import java.util.Map;
import net.hasor.dataway.service.DatawayException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HostExceptionAdvice {
    @ExceptionHandler(DatawayException.class)
    public ResponseEntity<Map<String, String>> handle(DatawayException error) {
        return ResponseEntity.status(error.status()).body(Map.of("message", error.getMessage()));
    }
}
