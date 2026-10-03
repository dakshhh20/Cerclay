package com.mittiandmore.service;

import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.ShipmentRepository;
import com.mittiandmore.repository.ShipmentTrackingEventRepository;
import com.mittiandmore.entity.ShipmentTrackingEvent;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ShadowfaxTrackingService {

    private final ShadowfaxClient shadowfaxClient;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ShipmentTrackingEventRepository trackingEventRepository;

    public ShadowfaxTrackingService(
            ShadowfaxClient shadowfaxClient,
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            NotificationService notificationService,
            ShipmentTrackingEventRepository trackingEventRepository
    ) {
        this.shadowfaxClient = shadowfaxClient;
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.trackingEventRepository = trackingEventRepository;
    }

    @Transactional
    public Shipment syncShipmentTracking(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId).orElseThrow(() ->
                new IllegalArgumentException("Shipment not found: " + shipmentId));

        validateShadowfaxShipment(shipment);
        String awb = shipment.getTrackingNumber();
        ShadowfaxClient.ShadowfaxTrackingResponse response = shadowfaxClient.trackOrder(awb);
        return applyTrackingResponse(shipment, response, "POLL");
    }

    private void validateShadowfaxShipment(Shipment shipment) {
        if (!"Shadowfax".equalsIgnoreCase(shipment.getCourierName())
                && !"SHADOWFAX".equalsIgnoreCase(shipment.getProviderCode())) {
            throw new IllegalStateException("Shipment is not assigned to Shadowfax");
        }
        if (shipment.getTrackingNumber() == null || shipment.getTrackingNumber().isBlank()) {
            throw new IllegalStateException("Shipment does not have a Shadowfax AWB");
        }
    }

    @Transactional(readOnly = true)
    public List<ShipmentTrackingEvent> getTrackingTimeline(Long shipmentId) {
        return trackingEventRepository.findByShipmentIdOrderByEventAtAscCreatedAtAsc(shipmentId);
    }

    /**
     * Applies a provider tracking response to a shipment. This is shared by
     * scheduled polling, the admin manual-sync fallback, and the webhook path.
     */
    @Transactional
    public Shipment applyTrackingResponse(Shipment shipment,
                                          ShadowfaxClient.ShadowfaxTrackingResponse response,
                                          String source) {
        if (response == null) {
            throw new IllegalStateException("Shadowfax returned an empty tracking response");
        }

        String previousStatus = shipment.getShipmentStatus();
        ShadowfaxClient.ShadowfaxTrackingOrderDetails details = response.order_details();

        if (details != null) {
            if (details.status() != null) {
                shipment.setExternalStatus(details.status());
                shipment.setShipmentStatus(mapShadowfaxStatus(details.status()));
            }
            shipment.setExternalStatusDisplay(details.status_display());
            shipment.setCustomerTrackUrl(details.customer_track_url());
            if (details.id() != null && shipment.getExternalShipmentId() == null) {
                shipment.setExternalShipmentId(String.valueOf(details.id()));
            }
        }

        List<ShadowfaxClient.ShadowfaxTrackingEvent> events = response.tracking_details();
        if (events == null) events = Collections.emptyList();

        for (ShadowfaxClient.ShadowfaxTrackingEvent event : events) {
            if (event == null) continue;
            LocalDateTime eventTime = parseShadowfaxDate(event.created());
            if (eventTime != null) shipment.setLastEventAt(eventTime);
            if (event.location() != null) shipment.setCurrentLocation(event.location());
            if (event.remarks() != null) shipment.setLatestTrackingComment(event.remarks());

            String eventStatus = event.status() != null ? event.status() : shipment.getExternalStatus();
            String normalized = mapShadowfaxStatus(eventStatus);
            String key = buildEventKey(shipment, event, normalized);
            if (!trackingEventRepository.existsByEventKey(key)) {
                ShipmentTrackingEvent record = new ShipmentTrackingEvent();
                record.setShipment(shipment);
                record.setEventKey(key);
                record.setExternalStatus(event.status() != null ? event.status() : shipment.getExternalStatus());
                record.setExternalStatusDisplay(shipment.getExternalStatusDisplay());
                record.setShipmentStatus(normalized);
                record.setLocation(event.location());
                record.setRemarks(event.remarks());
                record.setEventAt(eventTime);
                record.setSource(source);
                trackingEventRepository.save(record);
            }
        }

        shipment.setLastSyncedAt(LocalDateTime.now());
        updateShipmentTimestamps(shipment.getShipmentStatus(), shipment);
        synchronizeOrderDelivery(shipment);
        Shipment saved = shipmentRepository.save(shipment);

        if (!java.util.Objects.equals(previousStatus, saved.getShipmentStatus())) {
            NotificationEventType notificationEvent = switch (saved.getShipmentStatus()) {
                case "OUT_FOR_DELIVERY" -> NotificationEventType.ORDER_OUT_FOR_DELIVERY;
                case "DELIVERED" -> NotificationEventType.ORDER_DELIVERED;
                default -> null;
            };
            if (notificationEvent != null) {
                notificationService.enqueue(notificationEvent, saved.getOrder(), saved.getTrackingNumber(), saved.getCustomerTrackUrl());
            }
        }
        return saved;
    }

    private void synchronizeOrderDelivery(Shipment shipment) {
        if (!"DELIVERED".equals(shipment.getShipmentStatus())) return;
        Order order = shipment.getOrder();
        if (order == null) return;
        if (order.getDeliveredAt() == null) {
            order.setDeliveredAt(shipment.getDeliveredAt() != null ? shipment.getDeliveredAt() : LocalDateTime.now());
        }
        order.setOrderStatus("DELIVERED");
        orderRepository.save(order);
    }

    private String buildEventKey(Shipment shipment,
                                 ShadowfaxClient.ShadowfaxTrackingEvent event,
                                 String normalizedStatus) {
        String raw = String.join("|",
                String.valueOf(shipment.getId()),
                safe(event.created()),
                safe(event.status()),
                safe(event.status_id()),
                safe(event.location()),
                safe(event.remarks()),
                safe(normalizedStatus));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return "SHADOWFAX|" + hex;
        } catch (Exception ex) {
            return "SHADOWFAX|" + Integer.toHexString(raw.hashCode());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String mapShadowfaxStatus(
            String shadowfaxStatus
    ) {

        if (shadowfaxStatus == null) {
            return "CREATED";
        }

        return switch (
                shadowfaxStatus.trim().toLowerCase(Locale.ROOT)
                ) {

            case "new" ->
                    "CREATED";

            case "picked",
                 "assigned_for_seller_pickup",
                 "ofp" ->
                    "PICKED_UP";

            case "recd_at_rev_hub",
                 "item_manifested",
                 "recd_at_fwd_hub",
                 "recd_at_fwd_dc",
                 "assigned_for_delivery",
                 "bag_received",
                 "bag_in_transit",
                 "pincode_updated",
                 "item_misrouted",
                 "on_hold",
                 "reopen_ndr" ->
                    "IN_TRANSIT";

            case "ofd",
                 "rts_ofd" ->
                    "OUT_FOR_DELIVERY";

            case "delivered" ->
                    "DELIVERED";

            case "rts",
                 "rts_d",
                 "rts_in_process",
                 "rts_nd",
                 "in_transit_return" ->
                    "RETURNED";

            case "lost" ->
                    "LOST";

            case "cid",
                 "nc",
                 "na" ->
                    "DELIVERY_ATTEMPTED";

            default ->
                    "IN_TRANSIT";
        };
    }

    private void updateShipmentTimestamps(
            String shipmentStatus,
            Shipment shipment
    ) {

        if (shipmentStatus == null) {
            return;
        }

        if (
                "PICKED_UP".equals(shipmentStatus)
                        && shipment.getShippedAt() == null
        ) {
            shipment.setShippedAt(
                    LocalDateTime.now()
            );
        }

        if (
                "DELIVERED".equals(shipmentStatus)
                        && shipment.getDeliveredAt() == null
        ) {
            shipment.setDeliveredAt(
                    LocalDateTime.now()
            );

            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(
                        LocalDateTime.now()
                );
            }
        }
    }

    private LocalDateTime parseShadowfaxDate(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime
                    .parse(value)
                    .toLocalDateTime();

        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(
                    value,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );

        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(
                    value,
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss"
                    )
            );

        } catch (Exception ignored) {
        }

        return null;
    }
}