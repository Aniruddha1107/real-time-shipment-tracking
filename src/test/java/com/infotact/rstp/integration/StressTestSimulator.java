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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StressTestSimulator {

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

    private User carrier;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipmentRepository.deleteAll();
        userRepository.deleteAll();

        User shipper = User.builder()
                .email("shipper@test.com")
                .password("password")
                .name("Shipper")
                .role(Role.SHIPPER)
                .build();
        userRepository.save(shipper);

        carrier = User.builder()
                .email("carrier@test.com")
                .password("password")
                .name("Carrier")
                .role(Role.CARRIER)
                .build();
        carrier = userRepository.save(carrier);

        shipment = Shipment.builder()
                .title("Stress Test Shipment")
                .origin("A")
                .destination("B")
                .weight(1.0)
                .status(ShipmentStatus.IN_TRANSIT)
                .shipper(shipper)
                .awardedCarrier(carrier)
                .priceExpected(BigDecimal.TEN)
                .build();
        shipment = shipmentRepository.save(shipment);
    }

    @Test
    public void simulateConcurrentConnections() throws Exception {
        int connectionCount = 10;
        int pingsPerConnection = 5;
        CountDownLatch latch = new CountDownLatch(connectionCount * pingsPerConnection);
        AtomicInteger receivedCount = new AtomicInteger(0);

        List<StompSession> sessions = new ArrayList<>();
        String url = String.format("ws://localhost:%d/ws", port);

        UserDetails userDetails = userDetailsService.loadUserByUsername(carrier.getEmail());
        String jwt = jwtUtil.generateToken(userDetails);

        System.out.println("🚀 Starting Stress Test...");

        for (int i = 0; i < connectionCount; i++) {
            List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
            WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(transports));
            
            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setObjectMapper(objectMapper);
            client.setMessageConverter(converter);

            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer " + jwt);

            StompSession session = client.connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    session.subscribe("/topic/tracking/" + shipment.getShipmentId(), new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return TrackingEventDTO.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            receivedCount.incrementAndGet();
                            latch.countDown();
                        }
                    });
                }
            }).get(10, TimeUnit.SECONDS);

            sessions.add(session);
        }

        System.out.println("✅ Sessions connected.");

        for (int p = 0; p < pingsPerConnection; p++) {
            for (StompSession session : sessions) {
                TrackingEventDTO ping = TrackingEventDTO.builder()
                        .shipmentId(shipment.getShipmentId())
                        .carrierId(carrier.getId())
                        .latitude(40.0)
                        .longitude(-70.0)
                        .eventType("STRESS_TEST")
                        .build();
                session.send("/app/tracking.update", ping);
            }
        }

        boolean success = latch.await(25, TimeUnit.SECONDS);

        System.out.println("📊 Results: Sent " + (connectionCount * pingsPerConnection) + ", Received " + receivedCount.get());
        
        if (success) {
            System.out.println("🔥 STRESS TEST PASSED!");
        } else {
            System.out.println("❌ STRESS TEST FAILED OR TIMED OUT");
        }

        for (StompSession session : sessions) {
            session.disconnect();
        }
    }
}
