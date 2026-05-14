package com.infotact.rstp.controller;

import com.infotact.rstp.dto.NotificationDTO;
import com.infotact.rstp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final com.infotact.rstp.repository.UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/me/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.markAsRead(notificationId, getCurrentUserId(authentication));
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
    }
}
