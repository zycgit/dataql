/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE example_users (
    username VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    tenant VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL
);

-- PBKDF2-HMAC-SHA256, 210000 iterations, 256 bits. Demo password: example-password.
INSERT INTO example_users VALUES ('api', 'AmEZUHTD/T7cbGb5P+pZz+YCaFF6tUXSzK/NG917fPg=', 'LL9VA5fnri99GSSvjkynWQ==', 'api', 'example', TRUE);
INSERT INTO example_users VALUES ('reader', 'vZlIxXRPE/s/bRYpUV0nTDojBARZFsX9gYIgtLhfmh0=', 'Gnsxf9XnZpGVi9oPSGqH5A==', 'reader', 'example', TRUE);
INSERT INTO example_users VALUES ('admin', 'zz9l1ctCPhZzlmbE97Mzv0ubBIv2C4EWN3bKiUvMrrw=', 'sMyvm1VLvBBdnV/FQ1Pp3g==', 'admin', 'example', TRUE);
INSERT INTO example_users VALUES ('disabled', 'qbzA1lwjSSuxLmbLFlsU4MHb25wS1yTnU+NXa4wgyEk=', 'YpyxyeJ28S4VPb+ps+cYRw==', 'api', 'example', FALSE);
