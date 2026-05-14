package com.infotact.rstp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final com.infotact.rstp.security.JwtUtil jwtUtil;
    private final com.infotact.rstp.security.CustomUserDetailsService userDetailsService;
    private final com.infotact.rstp.repository.UserRepository userRepository;
    private final com.infotact.rstp.repository.ShipmentRepository shipmentRepository;

    public WebSocketConfig(com.infotact.rstp.security.JwtUtil jwtUtil,
                           com.infotact.rstp.security.CustomUserDetailsService userDetailsService,
                           com.infotact.rstp.repository.UserRepository userRepository,
                           com.infotact.rstp.repository.ShipmentRepository shipmentRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message, org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
                        org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (org.springframework.messaging.simp.stomp.StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);
                        String userEmail = jwtUtil.extractUsername(jwt);
                        if (userEmail != null) {
                            org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                                org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                            }
                        }
                    }
                }

                if (org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    assertTrackingSubscriptionAllowed(accessor);
                }
                return message;
            }
        });
    }

    private void assertTrackingSubscriptionAllowed(org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith("/topic/tracking/")) {
            return;
        }

        if (accessor.getUser() == null) {
            return;
        }

        Long shipmentId;
        try {
            shipmentId = Long.parseLong(destination.substring("/topic/tracking/".length()));
        } catch (NumberFormatException ex) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid tracking topic");
        }

        String email = accessor.getUser().getName();
        com.infotact.rstp.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unknown WebSocket user"));
        com.infotact.rstp.entity.Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Unknown shipment"));

        boolean canSubscribe = shipment.getShipper() != null && shipment.getShipper().getId().equals(user.getId());
        canSubscribe = canSubscribe || shipment.getAwardedCarrier() != null &&
                shipment.getAwardedCarrier().getId().equals(user.getId());

        if (!canSubscribe) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot subscribe to this shipment");
        }
    }
}
