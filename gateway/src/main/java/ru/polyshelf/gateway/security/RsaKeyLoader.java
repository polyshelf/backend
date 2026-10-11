package ru.polyshelf.gateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
public class RsaKeyLoader {

    private static final int GENERATED_KEY_SIZE = 2048;
    private static final String PEM_MARKERS = "-----(BEGIN|END) [A-Z ]+-----";

    private final ResourceLoader resourceLoader;

    public RsaKeyPair load(String privateKeyLocation, String publicKeyLocation, boolean allowEphemeral) {
        boolean hasPrivate = StringUtils.hasText(privateKeyLocation);
        boolean hasPublic = StringUtils.hasText(publicKeyLocation);

        if (!hasPrivate && !hasPublic) {
            if (!allowEphemeral) {
                throw new IllegalStateException("JWT keys are not configured: set POLYSHELF_JWT_PRIVATE_KEY and "
                        + "POLYSHELF_JWT_PUBLIC_KEY (ephemeral keys are allowed only with the 'local' profile)");
            }
            log.warn("JWT keys are not configured, generating an ephemeral RSA key pair. "
                    + "Issued tokens will become invalid after restart");
            return generate();
        }
        if (hasPrivate != hasPublic) {
            throw new IllegalStateException("JWT private and public keys must be configured together");
        }
        return new RsaKeyPair(readPublicKey(publicKeyLocation), readPrivateKey(privateKeyLocation));
    }

    private RsaKeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(GENERATED_KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            return new RsaKeyPair((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate RSA key pair", e);
        }
    }

    private RSAPrivateKey readPrivateKey(String location) {
        try {
            byte[] der = readDer(location);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to read JWT private key from " + location, e);
        }
    }

    private RSAPublicKey readPublicKey(String location) {
        try {
            byte[] der = readDer(location);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to read JWT public key from " + location, e);
        }
    }

    private byte[] readDer(String location) throws IOException {
        String pem = resourceLoader.getResource(location).getContentAsString(StandardCharsets.UTF_8);
        String base64 = pem.replaceAll(PEM_MARKERS, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
