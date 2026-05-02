package com.infotact.rstp.controller;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.service.BidService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public BidResponse placeBid(@RequestBody BidRequest request) {
        return bidService.placeBid(request);
    }

    @GetMapping("/shipment/{shipmentId}")
    public List<BidResponse> getBidsByShipment(@PathVariable Long shipmentId) {
        return bidService.getBidsByShipment(shipmentId);
    }

    @GetMapping("/carrier/{carrierId}")
    public List<BidResponse> getBidsByCarrier(@PathVariable Long carrierId) {
        return bidService.getBidsByCarrier(carrierId);
    }

    @PutMapping("/shipment/{shipmentId}/accept-lowest")
    public BidResponse acceptLowestBid(@PathVariable Long shipmentId) {
        return bidService.acceptLowestBid(shipmentId);
    }
}
