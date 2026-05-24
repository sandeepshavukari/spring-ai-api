package com.sandeep.service;

import com.sandeep.dto.request.UpdateProfileRequest;
import com.sandeep.dto.response.UserResponse;
import com.sandeep.exception.ResourceNotFoundException;
import com.sandeep.model.Role;
import com.sandeep.model.User;
import com.sandeep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getById(Long userId) {
        return AuthService.toUserResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getName() != null) user.setName(request.getName());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        user.setUpdatedAt(LocalDateTime.now());
        return AuthService.toUserResponse(userRepository.save(user));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AuthService::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateRoles(Long userId, Set<Role> roles) {
        User user = findUser(userId);
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        return AuthService.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
