package ru.polyshelf.gateway.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfiguration {

    @Bean
    public RsaKeyPair rsaKeyPair(JwtProperties properties, ResourceLoader resourceLoader) {
        return new RsaKeyLoader(resourceLoader).load(properties.privateKey(), properties.publicKey(),
                properties.allowEphemeralKeys());
    }

    @Bean
    public JwtEncoder jwtEncoder(RsaKeyPair keyPair) {
        RSAKey jwk = new RSAKey.Builder(keyPair.publicKey())
                .privateKey(keyPair.privateKey())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RsaKeyPair keyPair, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyPair.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
