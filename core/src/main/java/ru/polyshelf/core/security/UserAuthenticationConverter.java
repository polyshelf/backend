package ru.polyshelf.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class UserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));

        return new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

}