/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.dal.config;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;

/** Applies and validates Dataway database migrations before the data source is exposed. */
public class DwDbMigration {
    public static final String LOCATION      = "classpath:db/migration";
    public static final String HISTORY_TABLE = "dw_update_history";
    public static final String BASE_VERSION  = "202607170001";

    private final Flyway flyway;

    public DwDbMigration(DataSource dataSource) {
        this.flyway = Flyway.configure()    //
                .dataSource(dataSource)     //
                .locations(LOCATION)        //
                .table(HISTORY_TABLE)       //
                .baselineOnMigrate(true)    //
                .baselineVersion(MigrationVersion.fromVersion(BASE_VERSION))//
                .baselineDescription("Dataway baseline")//
                .outOfOrder(false)          //
                .load();
    }

    public void migrate() {
        this.flyway.migrate();
        this.flyway.validate();
    }
}
