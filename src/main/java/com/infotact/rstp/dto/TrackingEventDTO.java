package com.infotact.rstp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventDTO {
    private Long id;
    private Long shipmentId;
    private Long carrierId;
    private Double latitude;
    private Double longitude;
    private String locationDesc;
    private String eventType;
    private String notes;
    private LocalDateTime eventTimestamp;
}
