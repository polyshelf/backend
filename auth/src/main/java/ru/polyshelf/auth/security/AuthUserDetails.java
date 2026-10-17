package ru.polyshelf.auth.security;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.polyshelf.auth.models.UserEntity;

import java.util.Collection;
import java.util.List;

public record AuthUserDetails(UserEntity user) implements UserDetails {

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getEmail();
    }

}
