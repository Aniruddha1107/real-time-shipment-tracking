package com.infotact.rstp.service;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import java.util.List;

public interface BidService {
    BidResponse placeBid(BidRequest request, String carrierEmail);
    List<BidResponse> getBidsByShipment(Long shipmentId, String shipperEmail);
    List<BidResponse> getBidsByCarrier(String carrierEmail);
    BidResponse acceptLowestBid(Long shipmentId, String shipperEmail);
    BidResponse acceptBid(Long shipmentId, Long bidId, String shipperEmail);
}
