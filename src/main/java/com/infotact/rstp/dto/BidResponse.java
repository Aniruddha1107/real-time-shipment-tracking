package com.infotact.rstp.dto;

import com.infotact.rstp.entity.BidStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponse {

    private Long bidId;
    private Long shipmentId;
    private Long carrierId;
    private String carrierName;
    private BigDecimal bidPrice;
    private BidStatus status;
    private LocalDateTime bidTime;
}
