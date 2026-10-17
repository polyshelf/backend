package ru.polyshelf.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
        try (InputStream in = properties.privateKeyLocation().getInputStream()) {
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) RsaKeyConverters.pkcs8().convert(in);

            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));

            RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();

            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        }
    }

}
