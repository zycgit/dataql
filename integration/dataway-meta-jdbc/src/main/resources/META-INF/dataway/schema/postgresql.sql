/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE interface_info (
    api_id VARCHAR(64) COLLATE "C" NOT NULL PRIMARY KEY,
    api_method VARCHAR(12) COLLATE "C" NOT NULL,
    api_path VARCHAR(512) COLLATE "C" NOT NULL,
    api_status VARCHAR(4) COLLATE "C" NOT NULL,
    api_comment VARCHAR(255) COLLATE "C" NOT NULL,
    api_type VARCHAR(24) COLLATE "C" NOT NULL,
    api_script TEXT NOT NULL,
    api_schema TEXT NOT NULL,
    api_sample TEXT NOT NULL,
    api_option TEXT NOT NULL,
    api_create_time VARCHAR(32) NOT NULL,
    api_gmt_time VARCHAR(32) NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE interface_release (
    pub_id VARCHAR(64) COLLATE "C" NOT NULL PRIMARY KEY,
    pub_api_id VARCHAR(64) COLLATE "C" NOT NULL,
    pub_method VARCHAR(12) COLLATE "C" NOT NULL,
    pub_path VARCHAR(512) COLLATE "C" NOT NULL,
    pub_status VARCHAR(4) COLLATE "C" NOT NULL,
    pub_comment VARCHAR(255) COLLATE "C" NOT NULL,
    pub_type VARCHAR(24) COLLATE "C" NOT NULL,
    pub_script TEXT NOT NULL,
    pub_schema TEXT NOT NULL,
    pub_sample TEXT NOT NULL,
    pub_option TEXT NOT NULL,
    pub_release_time VARCHAR(32) NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
