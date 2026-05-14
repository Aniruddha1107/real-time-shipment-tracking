package com.infotact.rstp.controller;

import com.infotact.rstp.dto.BidRequest;
import com.infotact.rstp.dto.BidResponse;
import com.infotact.rstp.service.BidService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

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
    public BidResponse placeBid(@Valid @RequestBody BidRequest request, Authentication authentication) {
        return bidService.placeBid(request, authentication.getName());
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @GetMapping("/shipment/{shipmentId}")
    public List<BidResponse> getBidsByShipment(@PathVariable Long shipmentId, Authentication authentication) {
        return bidService.getBidsByShipment(shipmentId, authentication.getName());
    }

    @PreAuthorize("hasRole('CARRIER')")
    @GetMapping("/carrier/me")
    public List<BidResponse> getMyBids(Authentication authentication) {
        return bidService.getBidsByCarrier(authentication.getName());
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @PutMapping("/shipment/{shipmentId}/accept-lowest")
    public BidResponse acceptLowestBid(@PathVariable Long shipmentId, Authentication authentication) {
        return bidService.acceptLowestBid(shipmentId, authentication.getName());
    }

    @PreAuthorize("hasRole('SHIPPER')")
    @PutMapping("/shipment/{shipmentId}/bids/{bidId}/accept")
    public BidResponse acceptBid(@PathVariable Long shipmentId,
                                 @PathVariable Long bidId,
                                 Authentication authentication) {
        return bidService.acceptBid(shipmentId, bidId, authentication.getName());
    }
}
