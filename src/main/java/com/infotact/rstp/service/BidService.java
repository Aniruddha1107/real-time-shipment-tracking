package com.infotact.rstp.service;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import java.util.List;

public interface BidService {
    BidResponse placeBid(BidRequest request);
    List<BidResponse> getBidsByShipment(Long shipmentId);
    List<BidResponse> getBidsByCarrier(Long carrierId);
    BidResponse acceptLowestBid(Long shipmentId);
}
