# Dataway examples

Requires JDK 17 and Maven 3.9.9 or later. Import `example/pom.xml` to open all three applications, or import a child project's POM. Run the commands below from the repository root.

The examples use Maven artifact coordinates, independent of the repository's Gradle build. For the current Dataway snapshot, first run `./build.sh install` in the repository root to install its libraries and console resources into Maven Local. That source build also needs Node.js and npm. If using Hasor or dbVisitor snapshots, install the matching versions in those repositories as well, including `hasor-boot-maven-plugin` and `hasor-boot-loader` for the Hasor example. Versions are defined in `example/pom.xml`.

Build all examples, then start one executable JAR:

```bash
mvn -f example/pom.xml clean package
```

| Application | Start after packaging | Configuration |
| --- | --- | --- |
| [Spring Boot](dataway-spring-example) | `java -jar example/dataway-spring-example/target/dataway-spring-example.jar` | `src/main/resources/application.yml` |
| [Solon](dataway-solon-example) | `java -jar example/dataway-solon-example/target/dataway-solon-example.jar` | `src/main/resources/app.properties` |
| [Hasor Boot](dataway-hasor-example) | `java -jar example/dataway-hasor-example/target/dataway-hasor-example.jar` | `src/main/resources/hconfig.xml` |

Build one application with `mvn -f example/dataway-spring-example/pom.xml clean package`. The other applications use the same command with their own POM. Each module uses its framework's Maven packaging plugin; the resulting JAR includes runtime dependencies and Swagger UI assets.

Run one application at a time and open <http://127.0.0.1:8080/>. Sign in with `api`, `reader` or `admin`; password: `example-password`. Credentials, roles and enabled flags come from `database/users.sql`. The `disabled` account cannot sign in.

Each application includes JDBC metadata, H2 business databases (`ds1`, `ds2`), JWT cookie login, JSON/form APIs, file uploads and a local Swagger UI at `/swagger/index.html`. Explore the console at `/admin/` and API documents at `/docs/openapi.json`.

To use Nacos metadata:

```bash
docker compose -f example/compose.yaml up -d
# Wait for Nacos to finish starting, then:
java -jar example/dataway-spring-example/target/dataway-spring-example.jar --example.metadata=nacos
```

Solon and Hasor accept the same `--example.metadata=nacos` option. The local Compose service disables authentication, binds only to loopback, and uses separate dataIds for the three examples. Existing snapshots are preserved; missing demo snapshots are created with CAS. Startup waits for each written version to become readable before the next operation. Application users and business data still use H2.

See [the complete guide](../document/docs/dataway/configuration/examples.md) for configuration, uploads, transactions and authentication. Original integration tests remain under `integration/*/src/test`.
