package ru.polyshelf.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    private static final Pattern MIGRATION_FILE = Pattern.compile("V(\\d+)__.*\\.sql");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() throws IOException {
        var postgres = new PostgreSQLContainer("postgres:18");
        try (var files = Files.list(Path.of("../flyway/migration"))) {
            var migrations = files
                    .filter(Files::isRegularFile)
                    .filter(file -> MIGRATION_FILE.matcher(file.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(TestcontainersConfiguration::migrationVersion))
                    .toList();
            if (migrations.isEmpty()) {
                throw new IllegalStateException("No SQL migrations found for the test database");
            }
            for (int i = 0; i < migrations.size(); i++) {
                var migration = migrations.get(i);
                postgres.withCopyFileToContainer(MountableFile.forHostPath(migration),
                        "/docker-entrypoint-initdb.d/%05d-%s".formatted(i, migration.getFileName()));
            }
        }
        return postgres;
    }

    private static int migrationVersion(Path file) {
        var matcher = MIGRATION_FILE.matcher(file.getFileName().toString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a SQL migration: " + file);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
