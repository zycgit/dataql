/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE interface_info (
    api_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    api_method NVARCHAR(12) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_path NVARCHAR(512) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_status NVARCHAR(4) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_comment NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_type NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_script NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_schema NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_sample NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_option NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_create_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_gmt_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    api_revision BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE interface_release (
    pub_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL PRIMARY KEY,
    pub_api_id NVARCHAR(64) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_method NVARCHAR(12) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_path NVARCHAR(512) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_status NVARCHAR(4) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_comment NVARCHAR(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_type NVARCHAR(24) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_script NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_schema NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_sample NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_option NVARCHAR(MAX) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_release_time NVARCHAR(32) COLLATE Latin1_General_100_BIN2 NOT NULL,
    pub_revision BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE NONCLUSTERED INDEX uk_interface_info ON interface_info (api_method, api_path);
CREATE NONCLUSTERED INDEX idx_interface_release_api ON interface_release (pub_api_id);
CREATE NONCLUSTERED INDEX idx_interface_release_path ON interface_release (pub_method, pub_path, pub_status);
