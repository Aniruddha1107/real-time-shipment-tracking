package com.infotact.rstp.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@Slf4j
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        if (user != null) {
            log.info("Received a new web socket connection from user: {}", user.getName());
        } else {
            log.info("Received a new web socket connection (Unauthenticated)");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            log.info("User Disconnected: {}", user.getName());
            // Future enhancement: We can broadcast to a topic that the user went offline
            // messagingTemplate.convertAndSend("/topic/public", user.getName() + " left");
        } else {
            log.info("Unauthenticated User Disconnected");
        }
    }
}
