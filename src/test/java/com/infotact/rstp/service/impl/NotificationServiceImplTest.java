package com.infotact.rstp.service.impl;

import com.infotact.rstp.dto.NotificationDTO;
import com.infotact.rstp.entity.Notification;
import com.infotact.rstp.entity.NotificationType;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.NotificationRepository;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ShipmentRepository shipmentRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User mockUser;
    private Shipment mockShipment;
    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Test User");

        mockShipment = new Shipment();
        mockShipment.setShipmentId(100L);

        mockNotification = Notification.builder()
                .id(10L)
                .user(mockUser)
                .shipment(mockShipment)
                .message("Test message")
                .type(NotificationType.SYSTEM_ALERT)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateAndBroadcastNotification_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(shipmentRepository.findById(100L)).thenReturn(Optional.of(mockShipment));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Act
        notificationService.createAndBroadcastNotification(1L, 100L, "Hello", NotificationType.BID_RECEIVED);

        // Assert
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals("Hello", savedNotification.getMessage());
        assertEquals(NotificationType.BID_RECEIVED, savedNotification.getType());
        
        // Verify broadcast occurred
        verify(messagingTemplate).convertAndSend(eq("/topic/user/1"), any(NotificationDTO.class));
    }

    @Test
    void testGetUnreadNotifications_ReturnsList() {
        // Arrange
        when(notificationRepository.findByUserIdAndIsReadFalse(1L))
                .thenReturn(List.of(mockNotification));

        // Act
        List<NotificationDTO> result = notificationService.getUnreadNotifications(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test message", result.get(0).getMessage());
    }

    @Test
    void testMarkAsRead_Success() {
        // Arrange
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Act
        notificationService.markAsRead(10L, 1L);

        // Assert
        assertTrue(mockNotification.isRead());
        verify(notificationRepository).save(mockNotification);
    }
}
