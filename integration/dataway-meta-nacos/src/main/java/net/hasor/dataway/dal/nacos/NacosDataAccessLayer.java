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
import java.security.NoSuchAlgorithmException;
import java.util.*;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import net.hasor.dataway.dal.*;

/** Stores a complete metadata snapshot in one Nacos configuration using server-side CAS. */
public class NacosDataAccessLayer implements ApiDataAccessLayer {
    private final ConfigService configService;
    private final String        dataId;
    private final String        group;
    private final long          timeoutMillis;

    /** The caller owns the client; namespace, credentials and transport belong to that client. */
    public NacosDataAccessLayer(ConfigService configService, String dataId, String group, long timeoutMillis) {
        this.configService = Objects.requireNonNull(configService, "configService");
        this.dataId = requireName(dataId, "dataId");
        this.group = requireName(group, "group");
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        this.timeoutMillis = timeoutMillis;
    }

    private static String requireName(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public List<Map<FieldDef, String>> listObjects(EntityType entityType, Map<FieldDef, String> conditions) {
        NacosSnapshot.validateFields(entityType, conditions.keySet());
        NacosSnapshot snapshot = NacosSnapshot.parse(this.load(this.dataId, this.group));
        List<Map<FieldDef, String>> result = new ArrayList<>();
        snapshot.getRecords().get(entityType).values().stream().sorted(Comparator.comparing(row -> {
            return row.get(FieldDef.ID);
        })).filter(row -> {
            return conditions.entrySet().stream().allMatch(entry -> {
                return Objects.equals(row.get(entry.getKey()), entry.getValue());
            });
        }).forEach(row -> {
            result.add(new EnumMap<>(row));
        });
        return result;
    }

    @Override
    public void write(List<DataMutation> mutations) {
        if (mutations.isEmpty()) {
            return;
        }

        for (DataMutation mutation : mutations) {
            mutation.validate();
            NacosSnapshot.validateFields(mutation.getEntityType(), mutation.getFields().keySet());
        }

        String original = this.load(this.dataId, this.group);
        NacosSnapshot snapshot = NacosSnapshot.parse(original);
        for (DataMutation mutation : mutations) {
            snapshot.apply(mutation);
        }
        this.publish(original, snapshot);
    }

    /**
     * Imports the legacy INDEX_DIRECTORY_n and per-record configurations into an empty snapshot.
     * Stop all legacy writers first. Source configurations are read only and are never deleted.
     */
    public void importLegacy(String legacyGroup) {
        requireName(legacyGroup, "legacyGroup");
        String original = this.load(this.dataId, this.group);
        NacosSnapshot snapshot = NacosSnapshot.parse(original);
        if (snapshot.getRecords().values().stream().anyMatch(records -> !records.isEmpty())) {
            throw new DataConflictException("Legacy import requires an empty target snapshot");
        }
        for (DataMutation mutation : new LegacyNacosReader(this, legacyGroup).read()) {
            snapshot.apply(mutation);
        }

        this.publish(original, snapshot);
    }

    public String load(String configId, String configGroup) {
        try {
            return configService.getConfig(configId, configGroup, timeoutMillis);
        } catch (NacosException e) {
            throw new DataAccessException("Cannot read Nacos configuration " + configId, e);
        }
    }

    private void publish(String original, NacosSnapshot snapshot) {
        snapshot.setGeneration(UUID.randomUUID().toString());
        String content = snapshot.serialize();
        try {
            // Never publish without a previous digest: an unconditional write can lose concurrent changes.
            if (!configService.publishConfigCas(dataId, group, content, md5(original))) {
                throw new DataConflictException("Nacos snapshot changed or rejected; reload before retrying");
            }
        } catch (NacosException e) {
            // A timeout may occur after the server committed. Never blindly retry an ambiguous write.
            throw new DataAccessException("Nacos publish failed; reload to determine the outcome before retrying", e);
        }
    }

    private static String md5(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK has no MD5 implementation", e);
        }
    }
}
