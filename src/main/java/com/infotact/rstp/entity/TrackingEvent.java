package com.infotact.rstp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private User carrier;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "location_desc")
    private String locationDesc;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(length = 500)
    private String notes;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    protected void onCreate() {
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
    }
}
