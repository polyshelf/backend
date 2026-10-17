package ru.polyshelf.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../flyway/migration/V1__init.sql"),
                        "/docker-entrypoint-initdb.d/V1__init.sql");
    }

}
