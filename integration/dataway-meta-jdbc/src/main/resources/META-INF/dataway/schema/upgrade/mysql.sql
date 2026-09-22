/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
-- Stop all old and new Dataway writers before this upgrade. Replace table names for a custom prefix.
ALTER TABLE interface_info ADD COLUMN api_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE interface_release ADD COLUMN pub_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE interface_info DROP INDEX uk_interface_info;
ALTER TABLE interface_info MODIFY api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_info MODIFY api_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_info MODIFY api_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_release MODIFY pub_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_release MODIFY pub_method VARCHAR(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_release MODIFY pub_path VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE interface_release MODIFY pub_api_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
CREATE UNIQUE INDEX uk_interface_info ON interface_info (api_method, api_path);
