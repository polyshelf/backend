package ru.polyshelf.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthApplicationTests {

    private static final Path PRIVATE_KEY = TestKeys.writePrivateKey(TestKeys.generate());

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry registry) {
        registry.add("polyshelf.jwt.private-key-location", () -> "file:" + PRIVATE_KEY);
    }

    @Test
    void contextLoads() {
    }

}
