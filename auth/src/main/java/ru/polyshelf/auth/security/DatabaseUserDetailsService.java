package ru.polyshelf.auth.security;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.polyshelf.auth.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String email) {
        return userRepository.findByEmail(email)
                .map(AuthUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }

}
