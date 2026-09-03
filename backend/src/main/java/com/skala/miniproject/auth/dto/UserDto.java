package com.skala.miniproject.auth.dto;

import com.skala.miniproject.domain.user.Plan;
import com.skala.miniproject.domain.user.User;

import java.time.Instant;

public record UserDto(
        String id,
        String email,
        String name,
        String profileImageUrl,
        Plan plan,
        Instant createdAt
) {

    public static UserDto from(User user) {
        return new UserDto(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getPlan(),
                user.getCreatedAt()
        );
    }
}
