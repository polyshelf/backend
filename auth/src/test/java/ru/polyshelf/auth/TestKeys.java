package ru.polyshelf.auth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class TestKeys {

    private TestKeys() {
    }

    public static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Path writePrivateKey(KeyPair keyPair) {
        try {
            String body = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(keyPair.getPrivate().getEncoded());
            Path file = Files.createTempFile("jwt-test-", ".pem");
            file.toFile().deleteOnExit();
            Files.writeString(file, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n");
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
