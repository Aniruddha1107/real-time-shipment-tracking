package com.infotact.rstp.service;

import java.util.List;

import com.infotact.rstp.dto.NotificationDTO;
import com.infotact.rstp.entity.NotificationType;

public interface NotificationService {
    NotificationDTO createAndBroadcastNotification(Long userId, Long shipmentId, String message, NotificationType type);
    List<NotificationDTO> getUserNotifications(Long userId);
    List<NotificationDTO> getUnreadNotifications(Long userId);
    void markAsRead(Long notificationId, Long userId);
}
