package com.taskflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.taskflow.dto.user.ChangePasswordRequest;
import com.taskflow.dto.user.UpdateProfileRequest;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.security.RefreshTokenCookieService;
import com.taskflow.security.TaskFlowUserPrincipal;
import com.taskflow.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public UserController(
            UserService userService,
            RefreshTokenCookieService refreshTokenCookieService
    ) {
        this.userService = userService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal TaskFlowUserPrincipal principal) {
        return userService.getById(principal.id());
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(principal.id(), request);
    }

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal TaskFlowUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        userService.changePassword(principal.id(), request);
        refreshTokenCookieService.clear(response);
    }
}
