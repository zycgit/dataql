/*
 * Copyright 2015-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE.txt file for the full license.
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package net.hasor.dataway.spring.nacos.testcase;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

/** Owns an isolated, real Nacos process without adding server libraries to the application. */
public final class NacosTestServer implements AutoCloseable {
    private static final String  VERSION      = "3.1.2";
    private static final String  SHA256       = "b17a0846f846228c12a034beab977e0c3474468c1eca1c8765131316b353970b";
    private final        Path    directory;
    private final        int     port;
    private final        Thread  shutdownHook = new Thread(this::close, "dataway-test-nacos-shutdown");
    private              Process process;

    public NacosTestServer(Path target) throws Exception {
        Path cache = Files.createDirectories(target.toAbsolutePath().resolve("nacos"));
        Path archive = this.archive(cache);
        this.directory = Files.createTempDirectory(cache, "run-");
        this.port = this.availablePort();
        Path jar = cache.resolve("nacos-server-" + VERSION + ".jar");
        Path config = Files.createDirectories(this.directory.resolve("conf"));
        Files.createDirectories(this.directory.resolve("logs"));
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (!Files.isRegularFile(jar)) {
                Path temporary = Files.createTempFile(cache, "server-", ".jar");
                try (var input = zip.getInputStream(zip.getEntry("nacos/target/nacos-server.jar"))) {
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(temporary, jar, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            try (var input = zip.getInputStream(zip.getEntry("nacos/conf/nacos-logback.xml"))) {
                Files.copy(input, config.resolve("nacos-logback.xml"));
            }
        }
        try (var input = NacosTestServer.class.getResourceAsStream("/nacos.properties")) {
            if (input == null) {
                throw new IOException("Missing test resource nacos.properties");
            }
            Files.copy(input, config.resolve("application.properties"));
        }

        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Xms128m", "-Xmx512m", "-XX:ActiveProcessorCount=2",//
                "-Dnacos.standalone=true", "-Dnacos.deployment.type=server", "-Dnacos.functionMode=config",//
                "-Dnacos.remote.grpc.listen.ip=127.0.0.1", "-Dnacos.server.main.port=" + this.port,//
                "-Dnacos.home=" + this.directory, "-jar", jar.toString(),//
                "--spring.config.additional-location=file:" + config + "/",//
                "--logging.config=" + config.resolve("nacos-logback.xml").toUri());
        builder.directory(this.directory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(this.directory.resolve("logs/startup.log").toFile());
        this.process = builder.start();
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        try {
            this.awaitReady();
        } catch (Exception error) {
            this.close();
            throw error;
        }
        System.out.println("Test Nacos ready at " + this.serverAddress() + "; logs: " + this.directory.resolve("logs"));
    }

    private Path archive(Path cache) throws Exception {
        String localArchive = System.getProperty("nacos.test.archive");
        Path archive = localArchive == null ? cache.resolve("nacos-server-" + VERSION + ".zip") : Path.of(localArchive);
        if (!Files.isRegularFile(archive)) {
            if (localArchive != null) {
                throw new IOException("Nacos distribution does not exist: " + archive);
            }
            URI uri = URI.create("https://github.com/alibaba/nacos/releases/download/" + VERSION + "/nacos-server-" + VERSION + ".zip");
            System.out.println("Downloading test Nacos " + VERSION + " to " + archive);
            Path temporary = Files.createTempFile(cache, "download-", ".zip");
            try {
                HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
                HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).build();
                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temporary));
                if (response.statusCode() != 200) {
                    throw new IOException("Nacos download failed: HTTP " + response.statusCode() + ", " + uri);
                }
                this.checkArchive(temporary);
                Files.move(temporary, archive, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } else {
            this.checkArchive(archive);
        }
        return archive;
    }

    private void checkArchive(Path archive) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = new DigestInputStream(Files.newInputStream(archive), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        if (!SHA256.equals(HexFormat.of().formatHex(digest.digest()))) {
            throw new IOException("Nacos " + VERSION + " archive checksum mismatch: " + archive);
        }
    }

    private int availablePort() throws IOException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        for (int attempt = 0; attempt < 100; attempt++) {
            int candidate = ThreadLocalRandom.current().nextInt(20000, 45000);
            // Nacos derives its Raft and gRPC ports from the main port.
            try (var http = new ServerSocket(candidate, 1, address);//
                 var raft = new ServerSocket(candidate - 1000, 1, address);//
                 var client = new ServerSocket(candidate + 1000, 1, address);//
                 var cluster = new ServerSocket(candidate + 1001, 1, address)) {
                return candidate;
            } catch (IOException occupied) {
                // Try another group of ports without touching an existing server.
            }
        }
        throw new IOException("No free ports for test Nacos");
    }

    private void awaitReady() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", this.serverAddress());
        properties.setProperty("enableRemoteSyncConfig", "false");
        ConfigService client = NacosFactory.createConfigService(properties);
        try {
            long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
            NacosException lastFailure = null;
            boolean published = false;
            while (System.nanoTime() < deadline) {
                if (!this.process.isAlive()) {
                    throw new IOException("Nacos exited with code " + this.process.exitValue() + "; see " + this.directory.resolve("logs/startup.log"));
                }
                try {
                    if ("UP".equals(client.getServerStatus())) {
                        if (!published) {
                            published = client.publishConfig("readiness", "DATAWAY_TEST", "ready");
                        }
                        if (published && "ready".equals(client.getConfig("readiness", "DATAWAY_TEST", 1000))) {
                            client.removeConfig("readiness", "DATAWAY_TEST");
                            return;
                        }
                    }
                } catch (NacosException error) {
                    lastFailure = error;
                }
                Thread.sleep(100);
            }
            throw new IOException("Nacos did not become ready; see " + this.directory.resolve("logs/startup.log"), lastFailure);
        } finally {
            client.shutDown();
        }
    }

    public String serverAddress() {
        return "127.0.0.1:" + this.port;
    }

    public Path directory() {
        return this.directory;
    }

    @Override
    public synchronized void close() {
        if (this.process == null) {
            return;
        }
        this.process.destroy();
        try {
            if (!this.process.waitFor(10, TimeUnit.SECONDS)) {
                this.process.destroyForcibly();
                if (!this.process.waitFor(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Could not stop test Nacos process " + this.process.pid());
                }
            }
        } catch (InterruptedException error) {
            this.process.destroyForcibly();
            Thread.currentThread().interrupt();
        } finally {
            this.process = null;
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (IllegalStateException shuttingDown) {
                // Shutdown hooks are already running.
            }
        }
    }
}
