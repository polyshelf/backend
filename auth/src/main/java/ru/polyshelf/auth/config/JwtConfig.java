package ru.polyshelf.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) throws IOException, GeneralSecurityException {
        RSAPrivateCrtKey privateKey = privateKey(properties);
        RSAKey rsaKey = new RSAKey.Builder(publicKey(privateKey)).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) throws IOException, GeneralSecurityException {
        return NimbusJwtDecoder.withPublicKey(publicKey(privateKey(properties))).build();
    }

    private static RSAPrivateCrtKey privateKey(JwtProperties properties) throws IOException {
        try (InputStream in = properties.privateKeyLocation().getInputStream()) {
            return (RSAPrivateCrtKey) RsaKeyConverters.pkcs8().convert(in);
        }
    }

    private static RSAPublicKey publicKey(RSAPrivateCrtKey privateKey) throws GeneralSecurityException {
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
    }

}
