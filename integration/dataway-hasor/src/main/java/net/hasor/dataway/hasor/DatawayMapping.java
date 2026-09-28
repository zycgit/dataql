/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.hasor;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import net.hasor.core.BindInfo;
import net.hasor.web.binder.MappingDef;

/** Groups one entry's native mappings after its handler has been initialized. */
final class DatawayMapping extends MappingDef {
    private final List<MappingDef> mappings;

    DatawayMapping(BindInfo<DatawayController> controller, List<String> paths) {
        super(0, controller, paths.get(0), method -> true);
        this.mappings = paths.stream().map(path -> {
            return new MappingDef(0, controller, path, method -> true);
        }).toList();
    }

    @Override
    public boolean matchingMapping(HttpServletRequest request) {
        return this.mappings.stream().anyMatch(m -> {
            return m.matchingMapping(request);
        });
    }

    @Override
    public String getMappingTo() {
        return this.mappings.stream().map(MappingDef::getMappingTo).collect(Collectors.joining("|"));
    }

    @Override
    public String getMappingToMatches() {
        return this.mappings.stream().map(mapping -> {
            return "(?:" + mapping.getMappingToMatches() + ")";
        }).collect(Collectors.joining("|"));
    }
}
