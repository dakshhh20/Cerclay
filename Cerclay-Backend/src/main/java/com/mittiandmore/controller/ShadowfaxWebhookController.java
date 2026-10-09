package com.mittiandmore.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.repository.ShipmentRepository;
import com.mittiandmore.service.ShadowfaxClient;
import com.mittiandmore.service.ShadowfaxTrackingService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/shadowfax")
public class ShadowfaxWebhookController {

    private final ObjectMapper objectMapper;
    private final ShipmentRepository shipmentRepository;
    private final ShadowfaxTrackingService trackingService;
    private final String webhookSecret;

    public ShadowfaxWebhookController(
        ObjectMapper objectMapper,
        ShipmentRepository shipmentRepository,
        ShadowfaxTrackingService trackingService,
        @Value("${shadowfax.webhook-secret:}") String webhookSecret
    ) {
        this.objectMapper = objectMapper;
        this.shipmentRepository = shipmentRepository;
        this.trackingService = trackingService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
        @RequestHeader(value = "X-Shadowfax-Webhook-Secret", required = false) String providedSecret,
        @RequestBody JsonNode payload
    ) {
        if (webhookSecret != null && !webhookSecret.isBlank() && !webhookSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JsonNode root = payload;
        if (root.has("data") && root.get("data").isObject()) {
            root = root.get("data");
        }

        ShadowfaxClient.ShadowfaxTrackingResponse response = objectMapper.convertValue(
            root,
            ShadowfaxClient.ShadowfaxTrackingResponse.class
        );

        String awb = extractAwb(root, response);
        if (awb == null || awb.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Shipment shipment = shipmentRepository.findByTrackingNumber(awb).orElse(null);
        if (shipment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            trackingService.applyTrackingResponse(shipment, response, "WEBHOOK");
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String extractAwb(JsonNode root, ShadowfaxClient.ShadowfaxTrackingResponse response) {
        if (response.order_details() != null && response.order_details().awb_number() != null) {
            return response.order_details().awb_number();
        }

        String[] fields = { "awb_number", "awb", "tracking_number", "trackingNumber" };
        for (String field : fields) {
            JsonNode node = root.get(field);
            if (node != null && node.isValueNode() && !node.asText().isBlank()) {
                return node.asText();
            }
        }

        JsonNode details = root.get("tracking_details");
        if (details != null && details.isArray()) {
            for (JsonNode event : details) {
                JsonNode awb = event.get("awb_number");
                if (awb != null && !awb.asText().isBlank()) return awb.asText();
            }
        }
        return null;
    }
}
