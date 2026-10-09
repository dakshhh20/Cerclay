package com.mittiandmore.controller;

import com.mittiandmore.dto.StoreSettingsRequest;
import com.mittiandmore.dto.StoreSettingsResponse;
import com.mittiandmore.entity.StoreSettings;
import com.mittiandmore.service.StoreSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store-settings")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public ResponseEntity<StoreSettingsResponse> getSettings() {
        StoreSettings settings = storeSettingsService.getSettings();

        return ResponseEntity.ok(toResponse(settings));
    }

    @PutMapping
    public ResponseEntity<StoreSettingsResponse> updateSettings(@Valid @RequestBody StoreSettingsRequest request) {
        StoreSettings settings = storeSettingsService.updateSettings(
            request.getGstRate(),
            request.getShippingCharge(),
            request.getFreeShippingThreshold(),
            request.getMinimumOrderValue(),
            request.getWhatsappNumber()
        );

        return ResponseEntity.ok(toResponse(settings));
    }

    private StoreSettingsResponse toResponse(StoreSettings settings) {
        StoreSettingsResponse response = new StoreSettingsResponse();

        response.setId(settings.getId());
        response.setGstRate(settings.getGstRate());
        response.setShippingCharge(settings.getShippingCharge());
        response.setFreeShippingThreshold(settings.getFreeShippingThreshold());
        response.setMinimumOrderValue(settings.getMinimumOrderValue());
        response.setWhatsappNumber(settings.getWhatsappNumber());
        response.setUpdatedAt(settings.getUpdatedAt());

        return response;
    }
}
