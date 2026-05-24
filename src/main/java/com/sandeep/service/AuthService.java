package com.sandeep.service;

import com.sandeep.dto.request.LoginRequest;
import com.sandeep.dto.request.RegisterRequest;
import com.sandeep.dto.response.AuthResponse;
import com.sandeep.dto.response.UserResponse;
import com.sandeep.exception.BadRequestException;
import com.sandeep.exception.UnauthorizedException;
import com.sandeep.model.AuthProvider;
import com.sandeep.model.Role;
import com.sandeep.model.User;
import com.sandeep.repository.UserRepository;
import com.sandeep.security.CustomUserDetails;
import com.sandeep.security.CustomUserDetailsService;
import com.sandeep.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(request.getEmail());
        return buildAuthResponse(userDetails.getUser());
    }

    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.isValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        String email = tokenProvider.extractEmail(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        return buildAuthResponse(userDetails.getUser());
    }

    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return AuthResponse.builder()
                .accessToken(tokenProvider.generateAccessToken(userDetails))
                .refreshToken(tokenProvider.generateRefreshToken(userDetails))
                .user(toUserResponse(user))
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(user.getRoles())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .dailyRequestCount(user.getDailyRequestCount())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
