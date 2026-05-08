package com.infotact.rstp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.infotact.rstp.dto.AuthResponse;
import com.infotact.rstp.dto.LoginRequest;
import com.infotact.rstp.dto.RegisterRequest;
import com.infotact.rstp.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")   // ✅ FIX: "/" must be there
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * REGISTER USER
     * URL: POST http://localhost:8084/api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request received for: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * LOGIN USER
     * URL: POST http://localhost:8084/api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received for: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}