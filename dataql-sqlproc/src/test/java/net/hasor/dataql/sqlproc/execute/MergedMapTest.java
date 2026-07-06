/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.dataql.sqlproc.execute;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MergedMapTest {

    @Test
    public void testEmpty() {
        MergedMap<String, Object> map = new MergedMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("a"));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue("x"));
    }

    @Test
    public void testPutAndGet() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(1));
        assertFalse(map.isEmpty());
    }

    @Test
    public void testPutOverride() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        assertEquals(1, map.put("a", 2));
        assertEquals(2, map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testAppendMap() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        sub.put("c", 3);

        map.appendMap(sub, false);
        assertEquals(2, map.size());
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testAppendMapWithLock() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("x", 10);

        map.appendMap(sub, true);
        assertEquals(10, map.get("x"));

        // locked keys cannot be removed - remove returns null
        assertNull(map.remove("x"));
        // key still present, value unchanged
        assertTrue(map.containsKey("x"));
        assertEquals(10, map.get("x"));
    }

    @Test
    public void testClearWithLock() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new LinkedHashMap<>();
        sub.put("x", 10);

        map.appendMap(sub, true);
        map.clear();
        assertFalse(map.isEmpty());
        // locked entries are set to null, not removed
        assertTrue(map.containsKey("x"));
        assertNull(map.get("x"));
    }

    @Test
    public void testClearWithoutLock() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("x", 10);

        map.appendMap(sub, false);
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemove() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        assertEquals(1, map.remove("a"));
        assertFalse(map.containsKey("a"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveFromSubMap() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);

        map.appendMap(sub, false);
        assertEquals(2, map.remove("b"));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testPutAll() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);

        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testKeySet() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
    }

    @Test
    public void testValues() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        Collection<Object> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySet() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("a", 1);
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(2, entries.size());

        int count = 0;
        for (Map.Entry<String, Object> e : entries) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testPutIntoSubMap() {
        MergedMap<String, Object> map = new MergedMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("b", 2);
        map.appendMap(sub, false);

        // put should update the submap
        map.put("b", 20);
        assertEquals(20, sub.get("b"));
        assertEquals(20, map.get("b"));
    }

    @Test
    public void testNullAppend() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.appendMap(null, false);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMultipleSubMaps() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("root", 0);

        Map<String, Object> sub1 = new HashMap<>();
        sub1.put("a", 1);
        map.appendMap(sub1, false);

        Map<String, Object> sub2 = new HashMap<>();
        sub2.put("b", 2);
        map.appendMap(sub2, false);

        assertEquals(3, map.size());
        assertEquals(0, map.get("root"));
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testOverrideFromParent() {
        MergedMap<String, Object> map = new MergedMap<>();
        map.put("shared", "parent");

        Map<String, Object> sub = new HashMap<>();
        sub.put("shared", "child");
        map.appendMap(sub, false);

        // unmerged (parent) takes priority
        assertEquals("parent", map.get("shared"));
    }

    @Test
    public void testRemoveNonExistent() {
        MergedMap<String, Object> map = new MergedMap<>();
        assertNull(map.remove("nonexistent"));
    }
}
