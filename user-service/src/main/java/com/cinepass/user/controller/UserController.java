package com.cinepass.user.controller;

import com.cinepass.user.dto.ChangePasswordRequest;
import com.cinepass.user.dto.CreateUserRequest;
import com.cinepass.user.dto.UpdateProfileRequest;
import com.cinepass.user.dto.UpdateUserRequest;
import com.cinepass.user.dto.UserResponse;
import com.cinepass.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<Void> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = extractUserId(auth);
        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id
    ) {

        UserResponse response = userService.getUserById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        UserResponse response = userService.updateUser(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id
    ) {

        userService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication auth) {
        String principal = auth.getName();
        try {
            return Long.valueOf(principal);
        } catch (NumberFormatException e) {
            return userService.getUserByEmail(principal).getId();
        }
    }
}