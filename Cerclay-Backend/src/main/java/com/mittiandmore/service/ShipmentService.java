package com.mittiandmore.service;

import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.ShipmentRepository;
import com.mittiandmore.repository.ShipmentTrackingEventRepository;
import com.mittiandmore.entity.ShipmentTrackingEvent;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final ShipmentTrackingEventRepository trackingEventRepository;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            OrderRepository orderRepository,
            NotificationService notificationService,
            ShipmentTrackingEventRepository trackingEventRepository
    ) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.trackingEventRepository = trackingEventRepository;
    }

    @Transactional
    public Shipment createShipment(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ApiException(
                                "ORDER_NOT_FOUND",
                                "Order not found",
                                HttpStatus.NOT_FOUND
                        )
                );

        if (shipmentRepository.findByOrderId(orderId).isPresent()) {
            throw new ApiException(
                    "SHIPMENT_ALREADY_EXISTS",
                    "Shipment already exists for this order",
                    HttpStatus.CONFLICT
            );
        }

        Shipment shipment = new Shipment();

        shipment.setOrder(order);
        shipment.setShipmentStatus("CREATED");

        return shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentByOrderId(Long orderId) {

        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new ApiException(
                                "SHIPMENT_NOT_FOUND",
                                "Shipment not found for this order",
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentByTrackingNumber(
            String trackingNumber
    ) {

        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new ApiException(
                    "TRACKING_NUMBER_REQUIRED",
                    "Tracking number is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        return shipmentRepository
                .findByTrackingNumber(trackingNumber)
                .orElseThrow(() ->
                        new ApiException(
                                "SHIPMENT_NOT_FOUND",
                                "Shipment not found",
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    @Transactional(readOnly = true)
    public java.util.List<ShipmentTrackingEvent> getShipmentTimeline(Long shipmentId) {
        return trackingEventRepository.findByShipmentIdOrderByEventAtAscCreatedAtAsc(shipmentId);
    }

    @Transactional
    public Shipment updateShipmentStatus(
            Long shipmentId,
            String newStatus
    ) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new ApiException(
                                "SHIPMENT_NOT_FOUND",
                                "Shipment not found",
                                HttpStatus.NOT_FOUND
                        )
                );

        validateStatus(newStatus);

        String previousStatus = shipment.getShipmentStatus();
        shipment.setShipmentStatus(newStatus);

        if ("PICKED_UP".equals(newStatus)
                || "IN_TRANSIT".equals(newStatus)
                || "OUT_FOR_DELIVERY".equals(newStatus)) {

            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(LocalDateTime.now());
            }
        }

        if ("DELIVERED".equals(newStatus)) {

            if (shipment.getShippedAt() == null) {
                shipment.setShippedAt(LocalDateTime.now());
            }

            if (shipment.getDeliveredAt() == null) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }

            // Keep the order delivery timestamp/status in sync with the shipment.
            // Return eligibility is calculated from Order.deliveredAt.
            Order order = shipment.getOrder();
            if (order != null) {
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(shipment.getDeliveredAt());
                }
                order.setOrderStatus("DELIVERED");
                orderRepository.save(order);
            }
        }

        Shipment saved = shipmentRepository.save(shipment);
        if (!newStatus.equals(previousStatus)) {
            ShipmentTrackingEvent eventRecord = new ShipmentTrackingEvent();
            eventRecord.setShipment(saved);
            eventRecord.setEventKey("ADMIN|" + saved.getId() + "|" + System.nanoTime() + "|" + newStatus);
            eventRecord.setExternalStatus(saved.getExternalStatus());
            eventRecord.setExternalStatusDisplay(saved.getExternalStatusDisplay());
            eventRecord.setShipmentStatus(newStatus);
            eventRecord.setLocation(saved.getCurrentLocation());
            eventRecord.setRemarks("Shipment status changed from " + previousStatus + " to " + newStatus + " by admin.");
            eventRecord.setEventAt(LocalDateTime.now());
            eventRecord.setSource("ADMIN");
            trackingEventRepository.save(eventRecord);
            NotificationEventType event = switch (newStatus) {
                case "OUT_FOR_DELIVERY" -> NotificationEventType.ORDER_OUT_FOR_DELIVERY;
                case "DELIVERED" -> NotificationEventType.ORDER_DELIVERED;
                default -> null;
            };
            if (event != null) {
                notificationService.enqueue(event, saved.getOrder(), saved.getTrackingNumber(), saved.getCustomerTrackUrl());
            }
        }
        return saved;
    }

    @Transactional
    public Shipment assignCourier(
            Long shipmentId,
            String courierName,
            String trackingNumber
    ) {

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() ->
                        new ApiException(
                                "SHIPMENT_NOT_FOUND",
                                "Shipment not found",
                                HttpStatus.NOT_FOUND
                        )
                );

        if (courierName == null || courierName.isBlank()) {
            throw new ApiException(
                    "COURIER_NAME_REQUIRED",
                    "Courier name is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (trackingNumber != null
                && !trackingNumber.isBlank()) {

            Optional<Shipment> existingShipment =
                    shipmentRepository.findByTrackingNumber(
                            trackingNumber
                    );

            if (existingShipment.isPresent()
                    && !existingShipment.get()
                    .getId()
                    .equals(shipmentId)) {

                throw new ApiException(
                        "TRACKING_NUMBER_ALREADY_ASSIGNED",
                        "Tracking number is already assigned",
                        HttpStatus.CONFLICT
                );
            }
        }

        shipment.setCourierName(courierName);
        shipment.setTrackingNumber(trackingNumber);

        return shipmentRepository.save(shipment);
    }

    private void validateStatus(String status) {

        if (status == null || status.isBlank()) {
            throw new ApiException(
                    "SHIPMENT_STATUS_REQUIRED",
                    "Shipment status is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        switch (status) {
            case "CREATED",
                 "PICKED_UP",
                 "IN_TRANSIT",
                 "OUT_FOR_DELIVERY",
                 "DELIVERED",
                 "RETURNED",
                 "LOST",
                 "DELIVERY_ATTEMPTED" -> {
                // Valid status
            }

            default -> throw new ApiException(
                    "INVALID_SHIPMENT_STATUS",
                    "Invalid shipment status",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}