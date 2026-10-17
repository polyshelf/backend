package ru.polyshelf.auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.polyshelf.auth.models.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
