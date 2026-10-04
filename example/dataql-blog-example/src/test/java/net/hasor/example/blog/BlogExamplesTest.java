/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.example.blog;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.hasor.dataql.util.JsonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BlogExamplesTest {
    private final String name;

    public BlogExamplesTest(String name) {
        this.name = name;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> cases() {
        return Arrays.asList("tree-to-tree", "dataset-ranking", "like-for", "build-tree", "row-to-col", "left-join", "group-by", "dim-reduction");
    }

    @Test
    public void scriptMatchesPublishedResult() throws Exception {
        try (InputStream input = this.getClass().getResourceAsStream("/cases/" + this.name + "/expected.json")) {
            Object result = new BlogExamples().execute(this.name);
            this.assertJsonEquals(JsonUtils.readValue(new String(input.readAllBytes(), StandardCharsets.UTF_8), Object.class), JsonUtils.readValue(JsonUtils.writeValueAsString(result), Object.class));
        }
    }

    private void assertJsonEquals(Object expected, Object actual) {
        if (expected instanceof Number left && actual instanceof Number right) {
            assertEquals(0, new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())));
        } else if (expected instanceof Map<?, ?> left && actual instanceof Map<?, ?> right) {
            assertEquals(left.keySet(), right.keySet());
            left.forEach((key, value) -> this.assertJsonEquals(value, right.get(key)));
        } else if (expected instanceof List<?> left && actual instanceof List<?> right) {
            assertEquals(left.size(), right.size());
            for (int i = 0; i < left.size(); i++) {
                this.assertJsonEquals(left.get(i), right.get(i));
            }
        } else {
            assertEquals(expected, actual);
        }
    }
}
