package com.cinepass.user.services;

import com.cinepass.user.dto.CreateUserRequest;
import com.cinepass.user.dto.UpdateUserRequest;
import com.cinepass.user.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}