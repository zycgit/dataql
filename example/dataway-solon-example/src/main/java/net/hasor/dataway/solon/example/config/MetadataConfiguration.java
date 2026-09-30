/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.solon.example.config;
import java.util.Properties;
import javax.sql.DataSource;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import net.hasor.dataway.dal.ApiDataAccessLayer;
import net.hasor.dataway.dal.jdbc.JdbcDataAccessLayer;
import net.hasor.dataway.dal.nacos.NacosDataAccessLayer;
import net.hasor.dataway.dal.nacos.NacosSnapshot;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Destroy;
import org.noear.solon.core.AppContext;

/** Selects the demo's metadata provider and owns the optional Nacos client lifecycle. */
@Configuration
public class MetadataConfiguration {
    private ConfigService client;

    @Bean
    public ApiDataAccessLayer metadata(DataSource source, AppContext context) throws NacosException {
        var settings = context.cfg();
        String provider = settings.get("example.metadata", "jdbc");
        if ("jdbc".equalsIgnoreCase(provider)) {
            return new JdbcDataAccessLayer(source, "");
        }
        if (!"nacos".equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("example.metadata must be jdbc or nacos");
        }

        Properties properties = new Properties();
        properties.setProperty("serverAddr", settings.get("example.nacos.server-addr", "127.0.0.1:8848"));
        properties.setProperty("namespace", settings.get("example.nacos.namespace", ""));
        properties.setProperty("username", settings.get("example.nacos.username", ""));
        properties.setProperty("password", settings.get("example.nacos.password", ""));
        this.client = NacosFactory.createConfigService(properties);
        String dataId = settings.get("example.nacos.data-id", "dataway-solon-example");
        String group = settings.get("example.nacos.group", "DATAWAY_EXAMPLE");
        try {
            // Create only a missing demo snapshot. CAS prevents replacing an existing snapshot.
            if (this.client.getConfig(dataId, group, 5000) == null) {
                String emptyDigest = "d41d8cd98f00b204e9800998ecf8427e";
                this.client.publishConfigCas(dataId, group, NacosSnapshot.empty().serialize(), emptyDigest);
                this.awaitSnapshot(dataId, group);
            }
            return new NacosDataAccessLayer(this.client, dataId, group, 5000);
        } catch (NacosException | RuntimeException failure) {
            this.close();
            throw failure;
        }
    }

    // Nacos accepts writes before its query cache necessarily reflects the new configuration.
    private void awaitSnapshot(String dataId, String group) throws NacosException {
        for (int attempt = 0; attempt < 50; attempt++) {
            if (this.client.getConfig(dataId, group, 500) != null) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while initializing Nacos metadata", e);
            }
        }
        throw new IllegalStateException("Nacos snapshot was not readable after initialization");
    }

    @Destroy
    public void close() throws NacosException {
        if (this.client != null) {
            this.client.shutDown();
            this.client = null;
        }
    }
}
