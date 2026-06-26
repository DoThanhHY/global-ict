package com.globalict.iot_backend.service;

import com.globalict.iot_backend.Dto.AuthResponse;
import com.globalict.iot_backend.Dto.LoginRequest;
import com.globalict.iot_backend.entity.User;
import com.globalict.iot_backend.repository.UserRepository;
import com.globalict.iot_backend.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        String accessToken = jwtService.generateToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        log.info("User logged in: {}", request.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        String newAccessToken = jwtService.generateToken(username);
        String newRefreshToken = jwtService.generateRefreshToken(username);

        log.info("Token refreshed for user: {}", username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenBlacklistService.blacklistToken(accessToken);
        }
        if (refreshToken != null) {
            tokenBlacklistService.blacklistToken(refreshToken);
        }
        log.info("User logged out, tokens blacklisted");
    }

    public void register(LoginRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("User registered: {}", request.getUsername());
    }
}