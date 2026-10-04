/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.example.blog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.hasor.dataql.host.HostConfiguration;
import net.hasor.dataql.host.QueryManager;
import net.hasor.dataql.util.JsonUtils;

/** Runs one article's script with its accompanying parameters. */
public class BlogExamples {
    public static void main(String[] args) throws Exception {
        String name = args.length == 0 ? "build-tree" : args[0];
        Object result = new BlogExamples().execute(name);
        System.out.println(JsonUtils.writeValueAsPrettyString(result));
    }

    public Object execute(String name) throws Exception {
        String script = this.read("/cases/" + name + "/query.dql");
        Map<String, ?> parameters = JsonUtils.readValue(this.read("/cases/" + name + "/parameters.json"), Map.class);
        QueryManager manager = new QueryManager(new HostConfiguration());
        return manager.newBuilder().createQuery(script).execute(parameters).getData().unwrap();
    }

    private String read(String resource) throws IOException {
        try (InputStream input = BlogExamples.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Resource not found: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
