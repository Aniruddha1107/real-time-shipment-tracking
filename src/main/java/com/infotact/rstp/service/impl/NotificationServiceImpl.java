package com.infotact.rstp.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.infotact.rstp.dto.NotificationDTO;
import com.infotact.rstp.entity.Notification;
import com.infotact.rstp.entity.NotificationType;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.NotificationRepository;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ShipmentRepository shipmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public NotificationDTO createAndBroadcastNotification(Long userId, Long shipmentId, String message, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        Shipment shipment = null;
        if (shipmentId != null) {
            shipment = shipmentRepository.findById(shipmentId)
                    .orElse(null);
        }

        Notification notification = Notification.builder()
                .user(user)
                .shipment(shipment)
                .message(message)
                .type(type)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        NotificationDTO dto = mapToDTO(savedNotification);

        // Broadcast to specific user's topic
        messagingTemplate.convertAndSend("/topic/user/" + userId, dto);

        return dto;
    }

    @Override
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot update this notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .shipmentId(notification.getShipment() != null ? notification.getShipment().getShipmentId() : null)
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
