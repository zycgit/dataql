/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE IF NOT EXISTS example_orders
(
    id
    INT
    PRIMARY
    KEY,
    person_id
    INT
    NOT
    NULL,
    product
    VARCHAR
(
    100
) NOT NULL,
    amount DECIMAL
(
    12,
    2
) NOT NULL
    );

INSERT INTO example_orders
SELECT 101,
       1,
       'Keyboard',
       89.90 WHERE NOT EXISTS (SELECT 1 FROM example_orders WHERE id = 101);
INSERT INTO example_orders
SELECT 102,
       1,
       'Mouse',
       39.50 WHERE NOT EXISTS (SELECT 1 FROM example_orders WHERE id = 102);
INSERT INTO example_orders
SELECT 201,
       2,
       'Monitor',
       199.00 WHERE NOT EXISTS (SELECT 1 FROM example_orders WHERE id = 201);
