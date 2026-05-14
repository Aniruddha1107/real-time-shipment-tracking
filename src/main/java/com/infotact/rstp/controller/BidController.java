package com.infotact.rstp.controller;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.service.BidService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PreAuthorize("hasRole('CARRIER')")
    @PostMapping
    public BidResponse placeBid(@Valid @RequestBody BidRequest request) {
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

    @PreAuthorize("hasRole('SHIPPER')")
    @PutMapping("/shipment/{shipmentId}/accept-lowest")
    public BidResponse acceptLowestBid(@PathVariable Long shipmentId) {
        return bidService.acceptLowestBid(shipmentId);
    }
}
