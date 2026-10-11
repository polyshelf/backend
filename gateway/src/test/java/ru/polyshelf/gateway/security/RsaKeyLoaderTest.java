package ru.polyshelf.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class RsaKeyLoaderTest {

    private final RsaKeyLoader loader = new RsaKeyLoader(new DefaultResourceLoader());

    @Test
    void generatesEphemeralKeysOnlyWhenAllowed() {
        assertThat(loader.load("", "", true).publicKey()).isNotNull();
        assertThatThrownBy(() -> loader.load(null, null, false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requiresBothKeys() {
        assertThatThrownBy(() -> loader.load("file:/x", "", true)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readsPemFiles(@TempDir Path dir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Path privateFile = dir.resolve("private.pem");
        Path publicFile = dir.resolve("public.pem");
        Files.writeString(privateFile, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        Files.writeString(publicFile, pem("PUBLIC KEY", pair.getPublic().getEncoded()));

        RsaKeyPair loaded = loader.load(privateFile.toUri().toString(), publicFile.toUri().toString(), false);

        assertThat(loaded.publicKey()).isEqualTo(pair.getPublic());
        assertThat(loaded.privateKey()).isEqualTo(pair.getPrivate());
    }

    @Test
    void failsOnMissingFile(@TempDir Path dir) {
        String missing = dir.resolve("none.pem").toUri().toString();
        assertThatThrownBy(() -> loader.load(missing, missing, false)).isInstanceOf(IllegalStateException.class);
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }
}
