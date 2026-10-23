package ru.polyshelf.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.polyshelf.auth.models.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByYandexId(String yandexId);

}
