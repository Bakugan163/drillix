package com.example.drillix.user;

import java.time.LocalDateTime;

// В ответе НЕТ пароля (даже хэша) — нечего ему делать снаружи.
// Наша лекция про DTO в действии: наружу только безопасное
public record UserResponseDto(
        Long id,
        String username,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}