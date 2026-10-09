package com.mittiandmore.controller;

import com.mittiandmore.service.ShadowfaxClient;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shadowfax")
public class ShadowfaxServiceabilityController {

    private final ShadowfaxClient shadowfaxClient;

    public ShadowfaxServiceabilityController(ShadowfaxClient shadowfaxClient) {
        this.shadowfaxClient = shadowfaxClient;
    }

    @GetMapping("/serviceability/{pincode}")
    public ResponseEntity<List<ShadowfaxClient.ServiceabilityResponse>> checkServiceability(
        @PathVariable String pincode
    ) {
        return ResponseEntity.ok(shadowfaxClient.checkCustomerDeliveryServiceability(pincode));
    }
}
