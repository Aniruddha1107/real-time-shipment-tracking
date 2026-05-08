package com.infotact.rstp.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.infotact.rstp.dto.AuthResponse;
import com.infotact.rstp.dto.LoginRequest;
import com.infotact.rstp.dto.RegisterRequest;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.exception.EmailAlreadyExistsException;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.security.CustomUserDetailsService;
import com.infotact.rstp.security.JwtUtil;
import com.infotact.rstp.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    // ✅ REGISTER
    @Override
    public AuthResponse register(RegisterRequest request) {

        log.info("Registering new user with email: {}", request.getEmail());

        // ✅ CHECK EMAIL EXISTS
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new EmailAlreadyExistsException(
                    "Email already registered: " + request.getEmail()
            );
        }

        // ✅ CREATE USER
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(request.getRole())
                .build();

        // ✅ SAVE USER
        userRepository.save(user);

        log.info("User registered successfully: {}", request.getEmail());

        // ✅ GENERATE TOKEN
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(user.getEmail());

        String token = jwtUtil.generateToken(userDetails);

        // ✅ RESPONSE
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .userId(user.getId()) // 🔥 IMPORTANT
                .message("User registered successfully")
                .build();
    }

    // ✅ LOGIN
    @Override
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());

        // ✅ AUTHENTICATE
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // ✅ LOAD USER DETAILS
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getEmail());

        // ✅ GENERATE JWT TOKEN
        String token = jwtUtil.generateToken(userDetails);

        // ✅ GET USER
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow();

        log.info("Login successful for: {}", request.getEmail());

        // ✅ RESPONSE
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .userId(user.getId()) // 🔥 VERY IMPORTANT
                .message("Login successful")
                .build();
    }
}
