package com.infotact.rstp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private String email;

    private String role;

    // ✅ IMPORTANT
    private Long userId;

    private String message;
}