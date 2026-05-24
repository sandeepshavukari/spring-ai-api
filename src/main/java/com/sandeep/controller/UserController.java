package com.sandeep.controller;

import com.sandeep.dto.request.UpdateProfileRequest;
import com.sandeep.dto.response.ApiResponse;
import com.sandeep.dto.response.UserResponse;
import com.sandeep.security.CustomUserDetails;
import com.sandeep.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(Authentication auth) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getById(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = ((CustomUserDetails) auth.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }
}
