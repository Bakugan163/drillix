package com.example.drillix.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
     // Spring Security будет находить пользователя по логину.
    // Optional — пользователь может не существовать (неверный логин)
    Optional<User> findByUsername(String username);
}
