/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE IF NOT EXISTS example_people
(
    id
    INT
    PRIMARY
    KEY,
    name
    VARCHAR
(
    100
),
    balance INT
    );

INSERT INTO example_people
SELECT 1,
       'Alice',
       100 WHERE NOT EXISTS (SELECT 1 FROM example_people WHERE id = 1);
INSERT INTO example_people
SELECT 2,
       'Bob',
       200 WHERE NOT EXISTS (SELECT 1 FROM example_people WHERE id = 2);
