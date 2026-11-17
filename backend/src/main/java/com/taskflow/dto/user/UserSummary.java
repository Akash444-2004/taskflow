package com.taskflow.dto.user;

import com.taskflow.entity.User;

public record UserSummary(
        Long id,
        String fullName,
        String email
) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}
