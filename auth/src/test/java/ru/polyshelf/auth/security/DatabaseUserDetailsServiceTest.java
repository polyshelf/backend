package ru.polyshelf.auth.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.polyshelf.auth.models.UserEntity;
import ru.polyshelf.auth.repositories.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService service;

    @Test
    void mapsUserToUserDetailsWithRoleAuthority() {
        UserEntity user = new UserEntity();
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertEquals("user@example.com", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void throwsWhenUserMissing() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody@example.com"));
    }

}
