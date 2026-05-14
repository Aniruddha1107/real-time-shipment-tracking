package com.infotact.rstp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotact.rstp.dto.TrackingEventDTO;
import com.infotact.rstp.entity.Role;
import com.infotact.rstp.entity.Shipment;
import com.infotact.rstp.entity.ShipmentStatus;
import com.infotact.rstp.entity.User;
import com.infotact.rstp.repository.ShipmentRepository;
import com.infotact.rstp.repository.UserRepository;
import com.infotact.rstp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TrackingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private User carrier;
    private Shipment shipment;
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        // Clear DB
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        // Create Shipper
        User shipper = User.builder()
                .email("shipper@test.com")
                .password("password")
                .name("Shipper Test")
                .role(Role.SHIPPER)
                .build();
        shipper = userRepository.save(shipper);

        // Create Carrier
        carrier = User.builder()
                .email("carrier@test.com")
                .password("password")
                .name("Carrier Test")
                .role(Role.CARRIER)
                .build();
        carrier = userRepository.save(carrier);

        // Create Shipment
        shipment = Shipment.builder()
                .title("Test Shipment")
                .origin("City A")
                .destination("City B")
                .weight(10.5)
                .status(ShipmentStatus.IN_TRANSIT)
                .shipper(shipper)
                .awardedCarrier(carrier)
                .priceExpected(new BigDecimal("100.00"))
                .build();
        shipment = shipmentRepository.save(shipment);

        // Generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(carrier.getEmail());
        jwtToken = jwtUtil.generateToken(userDetails);

        // Setup STOMP client
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        stompClient = new WebSocketStompClient(new SockJsClient(transports));
        
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @Test
    public void testTrackingUpdateBroadcast() throws Exception {
        BlockingQueue<TrackingEventDTO> blockingQueue = new LinkedBlockingDeque<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/tracking/" + shipment.getShipmentId(), new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return TrackingEventDTO.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        blockingQueue.add((TrackingEventDTO) payload);
                    }
                });
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }
        };

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        StompSession session = stompClient
                .connectAsync(String.format("ws://localhost:%d/ws", port), new WebSocketHttpHeaders(), connectHeaders, sessionHandler)
                .get(10, TimeUnit.SECONDS);

        assertNotNull(session);

        // Send tracking update via WebSocket
        TrackingEventDTO ping = TrackingEventDTO.builder()
                .shipmentId(shipment.getShipmentId())
                .carrierId(carrier.getId())
                .latitude(45.523062)
                .longitude(-122.676482)
                .locationDesc("Portland, OR")
                .eventType("LOCATION_UPDATE")
                .build();

        session.send("/app/tracking.update", ping);

        // Wait for broadcast
        TrackingEventDTO received = blockingQueue.poll(15, TimeUnit.SECONDS);

        assertNotNull(received, "Should have received a tracking update message");
        assertEquals(ping.getLatitude(), received.getLatitude());
        assertEquals(ping.getLongitude(), received.getLongitude());
        assertEquals("Portland, OR", received.getLocationDesc());
    }
}
