/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.service.document;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.hasor.cobble.StringUtils;
import net.hasor.dataql.util.JsonUtils;
import net.hasor.dataway.service.DatawayException;

/** Reads the request/response schema and sample sections stored with an API release. */
final class DocumentSchema {
    private final Map<String, Object> schemas;
    private final Map<String, Object> samples;
    private final Map<String, String> references = new LinkedHashMap<>();

    public DocumentSchema(String schema, String sample) {
        this.schemas = this.object(schema);
        this.samples = this.object(sample);
    }

    public void registerSchemas(String apiID, Map<String, Object> target, boolean swagger) {
        String name = "api_" + Base64.getUrlEncoder().withoutPadding().encodeToString(apiID.getBytes(StandardCharsets.UTF_8));
        String prefix = swagger ? "#/definitions/" : "#/components/schemas/";
        for (String section : List.of("requestHeader", "requestBody", "responseHeader", "responseBody")) {
            Map<String, Object> schema = this.object(this.schemas.get(section));
            if (!schema.isEmpty()) {
                String key = name + "_" + section;
                Map<String, Object> rebased = this.object(this.references(schema, prefix + key));
                this.schemas.put(section, rebased);
                target.put(key, swagger ? this.swaggerSchema(rebased) : rebased);
                this.references.put(section, prefix + key);
            }
        }
    }

    /** Keeps local schema references local after moving a schema into the exported document. */
    private Object references(Object value, String root) {
        if (!(value instanceof Map<?, ?>) || this.object(value).containsKey("$id")) {
            return value;
        }
        Map<String, Object> schema = new LinkedHashMap<>(this.object(value));
        for (var entry : schema.entrySet()) {
            String key = entry.getKey();
            Object item = entry.getValue();
            if (("$ref".equals(key) || "$dynamicRef".equals(key)) && item instanceof String ref && ref.startsWith("#")) {
                if (!ref.equals("#") && !ref.startsWith("#/")) {
                    throw new DatawayException(422, "Use JSON Pointer references or an explicit schema $id for named anchors");
                }
                entry.setValue(root + ref.substring(1));
            } else if (Set.of("properties", "patternProperties", "$defs", "definitions", "dependentSchemas").contains(key)) {
                Map<String, Object> properties = new LinkedHashMap<>();
                this.object(item).forEach((name, property) -> properties.put(name, this.references(property, root)));
                entry.setValue(properties);
            } else if (Set.of("items", "additionalProperties", "propertyNames", "contains", "not", "if", "then", "else", "unevaluatedProperties", "unevaluatedItems").contains(key)) {
                entry.setValue(this.references(item, root));
            } else if (Set.of("allOf", "anyOf", "oneOf", "prefixItems").contains(key) && item instanceof List<?> alternatives) {
                entry.setValue(alternatives.stream().map(schemaItem -> this.references(schemaItem, root)).toList());
            }
        }
        return schema;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        if (value instanceof String text) {
            if (text.isBlank()) {
                return Map.of();
            }
            try {
                value = JsonUtils.readValue(text, Object.class);
            } catch (RuntimeException e) {
                throw new DatawayException(422, "Invalid API document metadata", e);
            }
        }
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?>)) {
            throw new DatawayException(422, "API document metadata must be a JSON object");
        }
        return (Map<String, Object>) value;
    }

    public boolean has(String section) {
        return this.schemas.containsKey(section) || this.hasSample(section);
    }

    public boolean hasSample(String section) {
        return this.samples.containsKey(section);
    }

    public Object sample(String section) {
        Object value = this.samples.get(section);
        if (value instanceof String text && !text.isBlank()) {
            try {
                return JsonUtils.readValue(text, Object.class);
            } catch (RuntimeException ignored) {
                // Samples may be plain text; unlike schemas, they need not contain JSON.
            }
        }
        return value;
    }

    public Map<String, Object> schema(String section, boolean swagger) {
        if (this.references.containsKey(section)) {
            return Map.of("$ref", this.references.get(section));
        }
        Map<String, Object> schema = this.object(this.schemas.get(section));
        return swagger ? this.swaggerSchema(schema) : schema;
    }

    private Map<String, Object> swaggerSchema(Map<String, Object> schema) {
        Map<String, Object> result = new LinkedHashMap<>(schema);
        result.remove("$schema");
        Set<String> supported = Set.of("$ref", "format", "title", "description", "default", "multipleOf", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type", "items", "allOf", "properties", "additionalProperties", "readOnly", "xml", "externalDocs", "example");
        for (var entry : result.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!supported.contains(key) && !key.startsWith("x-")) {
                throw new DatawayException(422, "Schema keyword is not supported by Swagger 2: " + key + "; use openapi.json");
            }
            if ("type".equals(key) && (!(value instanceof String) || "null".equals(value))) {
                throw new DatawayException(422, "Swagger 2 requires a single non-null schema type; use openapi.json");
            }
            if (("exclusiveMinimum".equals(key) || "exclusiveMaximum".equals(key)) && !(value instanceof Boolean)) {
                throw new DatawayException(422, "Swagger 2 requires boolean " + key + "; use openapi.json");
            }
            if ("properties".equals(key)) {
                Map<String, Object> properties = new LinkedHashMap<>();
                this.object(value).forEach((name, property) -> properties.put(name, this.swaggerSchema(this.object(property))));
                entry.setValue(properties);
            } else if ("items".equals(key) || ("additionalProperties".equals(key) && value instanceof Map<?, ?>)) {
                entry.setValue(this.swaggerSchema(this.object(value)));
            } else if ("allOf".equals(key) && value instanceof List<?> alternatives) {
                entry.setValue(alternatives.stream().map(item -> this.swaggerSchema(this.object(item))).toList());
            }
        }
        return result;
    }

    public Map<String, Object> content(String section) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schema", this.schema(section, false));
        if (this.hasSample(section)) {
            content.put("example", this.sample(section));
        }

        if ("requestBody".equals(section) && StringUtils.equalsIgnoreCase(this.contentType("requestHeader"), "application/x-www-form-urlencoded")) {
            Map<String, Object> properties = this.object(this.object(this.schemas.get(section)).get("properties"));
            Map<String, Object> encoding = new LinkedHashMap<>();
            for (var property : properties.entrySet()) {
                if ("array".equals(this.object(property.getValue()).get("type"))) {
                    // Declare repeated form fields explicitly for clients such as Swagger UI.
                    encoding.put(property.getKey(), Map.of("style", "form", "explode", true));
                }
            }

            if (!encoding.isEmpty()) {
                content.put("encoding", encoding);
            }
        }
        return content;
    }

    public List<Map<String, Object>> parameters(String section, String location, boolean swagger) {
        Map<String, Object> schema = this.object(this.schemas.get(section));
        Map<String, Object> properties = this.object(schema.get("properties"));
        Map<String, Object> examples = this.examples(section);
        Set<String> names = "header".equals(location) ? new TreeSet<>(String.CASE_INSENSITIVE_ORDER) : new LinkedHashSet<>();
        if ("header".equals(location)) {
            Map<String, Object> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitive.putAll(examples);
            examples = caseInsensitive;
        }
        names.addAll(properties.keySet());
        names.addAll(examples.keySet());
        List<?> required = schema.get("required") instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            if ("header".equals(location) && Set.of("accept", "content-type", "authorization").contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Map<String, Object> property = properties.containsKey(name) ? this.object(properties.get(name)) : Map.of("type", "string");
            Map<String, Object> parameter = new LinkedHashMap<>();
            parameter.put("name", name);
            parameter.put("in", location);
            parameter.put("required", required.contains(name));
            if (swagger) {
                parameter.putAll(this.swaggerParameter(property, "formData".equals(location)));
                if ("array".equals(parameter.get("type")) && !"header".equals(location)) {
                    parameter.put("collectionFormat", "multi");
                }
            } else {
                parameter.put("schema", property);
            }
            if (examples.containsKey(name)) {
                parameter.put(swagger ? "x-example" : "example", examples.get(name));
            }
            result.add(parameter);
        }
        return result;
    }

    private Map<String, Object> examples(String section) {
        Object sample = this.sample(section);
        if (sample instanceof List<?> headers) {
            Map<String, Object> examples = new LinkedHashMap<>();
            for (Object item : headers) {
                Map<String, Object> header = this.object(item);
                if (!Boolean.FALSE.equals(header.get("checked")) && header.get("name") instanceof String name) {
                    examples.put(name, header.get("value"));
                }
            }
            return examples;
        }
        return sample instanceof Map<?, ?> ? this.object(sample) : Map.of();
    }

    private Map<String, Object> swaggerParameter(Map<String, Object> schema, boolean form) {
        Map<String, Object> result = this.swaggerSchema(schema);
        Object type = result.get("type");
        if (form && "string".equals(type) && "binary".equals(result.get("format"))) {
            result.put("type", "file");
            result.remove("format");
            type = "file";
        }
        if (type == null || (!Set.of("string", "number", "integer", "boolean", "array").contains(type) && !(form && "file".equals(type)))) {
            throw new DatawayException(422, "Swagger 2 parameters require a primitive type or array; use openapi.json");
        }
        if ("array".equals(type)) {
            result.put("items", this.swaggerParameter(this.object(result.get("items")), false));
        }
        Set<String> allowed = Set.of("type", "format", "items", "default", "maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems", "enum", "multipleOf", "description");
        result.keySet().removeIf(key -> !allowed.contains(key) && !key.startsWith("x-"));
        return result;
    }

    public String contentType(String section) {
        for (var entry : this.examples(section).entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), "Content-Type") && entry.getValue() instanceof String value && !value.isBlank()) {
                return value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            }
        }
        return "application/json";
    }

    public Map<String, Object> responseHeaders(boolean swagger) {
        Map<String, Object> properties = this.object(this.object(this.schemas.get("responseHeader")).get("properties"));
        Map<String, Object> headers = new LinkedHashMap<>();
        for (var entry : properties.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), "Content-Type")) {
                continue;
            }
            Map<String, Object> schema = this.object(entry.getValue());
            headers.put(entry.getKey(), swagger ? this.swaggerParameter(schema, false) : Map.of("schema", schema));
        }
        return headers;
    }
}
