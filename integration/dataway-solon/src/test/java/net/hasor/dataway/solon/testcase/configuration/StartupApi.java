/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.testcase.configuration;
import net.hasor.dataway.model.ApiDefinition;
import net.hasor.dataway.model.ApiScriptType;
import net.hasor.dataway.service.Dataway;
import net.hasor.dataway.solon.DatawayPlugin;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Init;
import org.noear.solon.annotation.Inject;

/** An application initializer that consumes Dataway after its metadata storage is ready. */
@Component
public class StartupApi {
    @Inject
    private Dataway dataway;

    @Init(index = DatawayPlugin.INITIALIZATION_INDEX + 1)
    public void initialize() {
        ApiDefinition definition = new ApiDefinition();
        definition.setId("startup-api");
        definition.setMethod("GET");
        definition.setPath("/ready");
        definition.setType(ApiScriptType.DATA_QL);
        definition.setScript("return 'ready';");
        definition.setDescription("Published during host initialization");
        definition.setSchema("{}");
        definition.setSample("{}");
        definition.setOptions("{\"resultStructure\":false}");
        this.dataway.getAdminService().save(definition, 0);
        this.dataway.getAdminService().publish(definition.getId(), 1);
    }
}
