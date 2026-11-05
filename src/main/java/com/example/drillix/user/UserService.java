package com.example.drillix.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }

    public UserResponseDto create(UserRequestDto request) {
        // Логин занят? База не даст (unique), но дадим человеческую ошибку ДО падения
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalStateException(
                    "Логин '" + request.username() + "' уже занят");
        }

        User user = new User();
        user.setUsername(request.username());
        // ГЛАВНОЕ: хэшируем здесь. Открытый пароль не сохраняется никогда —
        // в базу ложится только bcrypt-хэш
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        return UserResponseDto.from(userRepository.save(user));
    }

    public List<UserResponseDto> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .toList();
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}