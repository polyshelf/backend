package ru.polyshelf.it;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import ru.polyshelf.it.Api.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

final class PolyshelfStack implements AutoCloseable {

    private static final String JRE_IMAGE = "eclipse-temurin:25-jre";
    private static final String APP_JAR = "/app/app.jar";

    private static final String DB_NAME = "polyshelf_db";
    private static final String DB_USER = "polyshelf_user";
    private static final String DB_PASSWORD = "polyshelf_password";

    private static final int GATEWAY_PORT = 8080;
    private static final int WIREMOCK_PORT = 8080;
    private static final int CORE_PORT = 8081;
    private static final int AUTH_PORT = 8082;
    private static final int ADAPTER_PORT = 8083;

    private static final String KEYS_DIR = "/run/keys/";
    private static final int KEY_FILE_MODE = 420;

    private static final Pattern MIGRATION_FILE = Pattern.compile("V(\\d+)__.*\\.sql");
    private static final String POSTGRES_INIT_DIR = "/docker-entrypoint-initdb.d/";

    private final Path repoRoot = Path.of(System.getProperty("repo.root"));
    private final Network network = Network.newNetwork();
    private final JwtKeys jwtKeys = new JwtKeys();

    private final PostgreSQLContainer postgres;
    private final GenericContainer<?> wiremock;
    private final GenericContainer<?> adapter;
    private final GenericContainer<?> auth;
    private final GenericContainer<?> core;
    private final GenericContainer<?> gateway;

    PolyshelfStack() {
        postgres = new PostgreSQLContainer("postgres:18")
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASSWORD)
                .withNetwork(network)
                .withNetworkAliases("postgres");
        copyMigrations(postgres, repoRoot.resolve("flyway").resolve("migration"));

        wiremock = new GenericContainer<>("wiremock/wiremock:3.9.1")
                .withCommand("--disable-http2-plain")
                .withNetwork(network)
                .withNetworkAliases("wiremock")
                .withExposedPorts(WIREMOCK_PORT)
                .waitingFor(Wait.forHttp("/__admin/mappings").forStatusCode(200));

        Map<String, String> database = Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/" + DB_NAME,
                "SPRING_DATASOURCE_USERNAME", DB_USER,
                "SPRING_DATASOURCE_PASSWORD", DB_PASSWORD);

        adapter = springApp("datamatrix-adapter", "adapter",
                Map.of("EXTERNAL_API_BASE_URL", "http://wiremock:" + WIREMOCK_PORT));

        auth = springApp("auth", "auth", with(database,
                "JWT_PRIVATE_KEY_LOCATION", "file:" + KEYS_DIR + "private.pem"))
                .withCopyFileToContainer(
                        MountableFile.forHostPath(jwtKeys.privateKey(), KEY_FILE_MODE),
                        KEYS_DIR + "private.pem");

        core = springApp("core", "core", with(database,
                "JWT_PUBLIC_KEY_LOCATION", "file:" + KEYS_DIR + "public.pem",
                "DATAMATRIX_ADAPTER_URL", "http://adapter:" + ADAPTER_PORT))
                .withCopyFileToContainer(
                        MountableFile.forHostPath(jwtKeys.publicKey(), KEY_FILE_MODE),
                        KEYS_DIR + "public.pem");

        gateway = springApp("gateway", "gateway", Map.of(
                "POLYSHELF_CORE_URL", "http://core:" + CORE_PORT,
                "POLYSHELF_AUTH_URL", "http://auth:" + AUTH_PORT))
                .withExposedPorts(GATEWAY_PORT);
    }

    void start() {
        for (GenericContainer<?> container : startupOrder()) {
            container.start();
        }
    }

    String gatewayUrl() {
        return url(gateway, GATEWAY_PORT);
    }

    String wiremockUrl() {
        return url(wiremock, WIREMOCK_PORT);
    }

    InventoryDb database() {
        return new InventoryDb(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Override
    public void close() {
        startupOrder().reversed().forEach(GenericContainer::stop);
        network.close();
        jwtKeys.close();
    }

    private List<GenericContainer<?>> startupOrder() {
        return List.of(postgres, wiremock, adapter, auth, core, gateway);
    }

    private GenericContainer<?> springApp(String module, String alias, Map<String, String> env) {
        return new GenericContainer<>(JRE_IMAGE)
                .withCopyFileToContainer(MountableFile.forHostPath(bootJar(module)), APP_JAR)
                .withCommand("java", "-XX:MaxRAMPercentage=75", "-jar", APP_JAR)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withEnv(env)
                .withLogConsumer(frame -> System.out.print(alias + " | " + frame.getUtf8String()))
                .waitingFor(Wait.forLogMessage(".*Started .* in .* seconds.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
    }

    private static String url(GenericContainer<?> container, int port) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(port);
    }

    private static Map<String, String> with(Map<String, String> base, String... extraKeyValues) {
        Map<String, String> result = new HashMap<>(base);
        for (int i = 0; i < extraKeyValues.length; i += 2) {
            result.put(extraKeyValues[i], extraKeyValues[i + 1]);
        }
        return result;
    }

    // ---- boot jars -------------------------------------------------------------------------------

    private Path bootJar(String module) {
        Path libs = repoRoot.resolve(module).resolve("build").resolve("libs");
        try (Stream<Path> files = Files.list(libs)) {
            return files
                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .filter(file -> !file.getFileName().toString().endsWith("-plain.jar"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No boot jar in " + libs + ", run :" + module + ":bootJar"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyMigrations(GenericContainer<?> postgres, Path migrationsDir) {
        List<Path> migrations;
        try (Stream<Path> files = Files.list(migrationsDir)) {
            migrations = files
                    .filter(file -> MIGRATION_FILE.matcher(file.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(PolyshelfStack::migrationVersion))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (int i = 0; i < migrations.size(); i++) {
            Path migration = migrations.get(i);
            postgres.withCopyFileToContainer(
                    MountableFile.forHostPath(migration),
                    POSTGRES_INIT_DIR + "%02d-%s".formatted(i, migration.getFileName()));
        }
    }

    private static int migrationVersion(Path file) {
        Matcher matcher = MIGRATION_FILE.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a migration: " + file);
        }
        return Integer.parseInt(matcher.group(1));
    }

    static final class InventoryDb {

        private static final String FIND_ITEM = """
                select i.custom_name, i.expiration_date, i.consumed_at, i.deleted_at
                from inventory_item i
                join app_user u on u.id = i.user_id
                where u.email = ? and i.id = ?
                """;

        private final String jdbcUrl;
        private final String username;
        private final String password;

        private InventoryDb(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        StoredItem find(User user, String itemId) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement statement = connection.prepareStatement(FIND_ITEM)) {
                statement.setString(1, user.email());
                statement.setObject(2, UUID.fromString(itemId));
                try (ResultSet rs = statement.executeQuery()) {
                    assertThat(rs.next()).as("inventory row %s of %s", itemId, user.email()).isTrue();
                    return new StoredItem(
                            rs.getString("custom_name"),
                            rs.getObject("expiration_date", LocalDate.class),
                            rs.getObject("consumed_at", OffsetDateTime.class),
                            rs.getObject("deleted_at", OffsetDateTime.class));
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to read inventory row " + itemId, e);
            }
        }

        record StoredItem(String customName, LocalDate expirationDate, OffsetDateTime consumedAt,
                          OffsetDateTime deletedAt) {

            boolean isConsumed() {
                return consumedAt != null;
            }
        }
    }

    private static final class JwtKeys implements AutoCloseable {

        private final Path directory;

        JwtKeys() {
            KeyPair keyPair = generate();
            try {
                directory = Files.createTempDirectory("polyshelf-it-keys-");
                Files.writeString(privateKey(), pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
                Files.writeString(publicKey(), pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Path privateKey() {
            return directory.resolve("private.pem");
        }

        Path publicKey() {
            return directory.resolve("public.pem");
        }

        @Override
        public void close() {
            try {
                Files.deleteIfExists(privateKey());
                Files.deleteIfExists(publicKey());
                Files.deleteIfExists(directory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static KeyPair generate() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return generator.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String pem(String type, byte[] der) {
            String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
            return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        }
    }
}
