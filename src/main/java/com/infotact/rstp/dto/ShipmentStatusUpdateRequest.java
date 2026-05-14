package com.infotact.rstp.dto;

import com.infotact.rstp.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipmentStatusUpdateRequest {

    @NotNull(message = "Shipment status is required")
    private ShipmentStatus status;
}
