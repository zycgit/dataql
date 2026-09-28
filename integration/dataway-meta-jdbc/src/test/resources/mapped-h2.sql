/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE SCHEMA metadata;

CREATE TABLE metadata.drafts
(
    definition_id    VARCHAR(64)  NOT NULL PRIMARY KEY,
    request_method   VARCHAR(12)  NOT NULL,
    route_path       VARCHAR(512) NOT NULL,
    lifecycle_status VARCHAR(4)   NOT NULL,
    description      VARCHAR(255) NOT NULL,
    script_type      VARCHAR(24)  NOT NULL,
    source_text      CLOB         NOT NULL,
    schema_document  CLOB         NOT NULL,
    sample_document  CLOB         NOT NULL,
    option_document  CLOB         NOT NULL,
    created_at       VARCHAR(32)  NOT NULL,
    updated_at       VARCHAR(32)  NOT NULL,
    row_version      BIGINT       NOT NULL DEFAULT 1,
    UNIQUE (request_method, route_path)
);

CREATE TABLE metadata.releases
(
    release_id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    definition_ref      VARCHAR(64)  NOT NULL,
    http_method         VARCHAR(12)  NOT NULL,
    http_path           VARCHAR(512) NOT NULL,
    release_status      VARCHAR(4)   NOT NULL,
    release_description VARCHAR(255) NOT NULL,
    release_type        VARCHAR(24)  NOT NULL,
    original_script     CLOB         NOT NULL,
    release_schema      CLOB         NOT NULL,
    release_sample      CLOB         NOT NULL,
    release_options     CLOB         NOT NULL,
    published_at        VARCHAR(32)  NOT NULL,
    release_version     BIGINT       NOT NULL DEFAULT 1
);
