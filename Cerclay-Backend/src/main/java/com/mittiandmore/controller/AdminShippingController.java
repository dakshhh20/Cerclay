package com.mittiandmore.controller;

import com.mittiandmore.dto.*;
import com.mittiandmore.service.ShippingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipping")
public class AdminShippingController {

    private final ShippingService shippingService;

    public AdminShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping("/zones")
    public ResponseEntity<List<ShippingZoneResponse>> getZones() {
        return ResponseEntity.ok(shippingService.getZones());
    }

    @PostMapping("/zones")
    public ResponseEntity<ShippingZoneResponse> createZone(@Valid @RequestBody ShippingZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.createZone(request));
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<ShippingZoneResponse> updateZone(
        @PathVariable Long id,
        @Valid @RequestBody ShippingZoneRequest request
    ) {
        return ResponseEntity.ok(shippingService.updateZone(id, request));
    }

    @GetMapping("/pincodes")
    public ResponseEntity<List<ShippingPincodeResponse>> getPincodes() {
        return ResponseEntity.ok(shippingService.getPincodes());
    }

    @PostMapping("/pincodes")
    public ResponseEntity<ShippingPincodeResponse> createPincode(@Valid @RequestBody ShippingPincodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shippingService.createPincode(request));
    }

    @PutMapping("/pincodes/{id}")
    public ResponseEntity<ShippingPincodeResponse> updatePincode(
        @PathVariable Long id,
        @Valid @RequestBody ShippingPincodeRequest request
    ) {
        return ResponseEntity.ok(shippingService.updatePincode(id, request));
    }

    @DeleteMapping("/pincodes/{id}")
    public ResponseEntity<Void> deletePincode(@PathVariable Long id) {
        shippingService.deletePincode(id);
        return ResponseEntity.noContent().build();
    }
}
