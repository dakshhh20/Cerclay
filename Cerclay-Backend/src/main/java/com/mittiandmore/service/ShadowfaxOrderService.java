package com.mittiandmore.service;

import com.mittiandmore.config.ShadowfaxProperties;
import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.OrderItem;
import com.mittiandmore.entity.Shipment;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.ShipmentRepository;
import com.mittiandmore.repository.ShipmentTrackingEventRepository;
import com.mittiandmore.entity.ShipmentTrackingEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ShadowfaxOrderService {

    private static final String COURIER_NAME = "Shadowfax";
    private static final String PROVIDER_CODE = "SHADOWFAX";

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShadowfaxClient shadowfaxClient;
    private final ShadowfaxProperties properties;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final ShadowfaxTrackingService shadowfaxTrackingService;

    public ShadowfaxOrderService(
            OrderRepository orderRepository,
            ShipmentRepository shipmentRepository,
            ShadowfaxClient shadowfaxClient,
            ShadowfaxProperties properties,
            ShipmentTrackingEventRepository trackingEventRepository,
            ShadowfaxTrackingService shadowfaxTrackingService
    ) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.shadowfaxClient = shadowfaxClient;
        this.properties = properties;
        this.trackingEventRepository = trackingEventRepository;
        this.shadowfaxTrackingService = shadowfaxTrackingService;
    }

    /**
     * Links an AWB created manually in the Shadowfax dashboard to an existing
     * Cerclay order. The local shipment is created if it does not exist yet.
     * The caller may then immediately ask Shadowfax for the latest status.
     */
    @Transactional
    public Shipment assignManualShadowfaxAwb(Long orderId, String awb) {
        if (orderId == null) {
            throw new ApiException(
                    "ORDER_ID_REQUIRED",
                    "Order ID is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (awb == null || awb.isBlank()) {
            throw new ApiException(
                    "SHADOWFAX_AWB_REQUIRED",
                    "Shadowfax AWB is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        String normalizedAwb = awb.trim();
        if (normalizedAwb.length() > 100) {
            throw new ApiException(
                    "SHADOWFAX_AWB_TOO_LONG",
                    "Shadowfax AWB must be at most 100 characters",
                    HttpStatus.BAD_REQUEST
            );
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ApiException(
                        "ORDER_NOT_FOUND",
                        "Order not found",
                        HttpStatus.NOT_FOUND
                ));

        String orderStatus = order.getOrderStatus();
        if (orderStatus != null) {
            String normalizedStatus = orderStatus.trim().toUpperCase(Locale.ROOT);
            if ("CANCELLED".equals(normalizedStatus)
                    || "CANCELED".equals(normalizedStatus)
                    || "DELIVERED".equals(normalizedStatus)) {
                throw new ApiException(
                        "ORDER_NOT_ELIGIBLE_FOR_SHIPMENT",
                        "Order is not eligible for a Shadowfax shipment",
                        HttpStatus.CONFLICT
                );
            }
        }

        Shipment existingByAwb = shipmentRepository.findByTrackingNumber(normalizedAwb).orElse(null);
        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);

        if (existingByAwb != null
                && (shipment == null || !existingByAwb.getId().equals(shipment.getId()))) {
            throw new ApiException(
                    "TRACKING_NUMBER_ALREADY_ASSIGNED",
                    "This Shadowfax AWB is already assigned to another order",
                    HttpStatus.CONFLICT
            );
        }

        if (shipment == null) {
            shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setShipmentStatus("CREATED");
        } else if (shipment.getTrackingNumber() != null
                && !shipment.getTrackingNumber().isBlank()
                && !normalizedAwb.equalsIgnoreCase(shipment.getTrackingNumber().trim())) {
            throw new ApiException(
                    "TRACKING_NUMBER_ALREADY_ASSIGNED",
                    "A different tracking number is already assigned to this order",
                    HttpStatus.CONFLICT
            );
        }

        shipment.setCourierName(COURIER_NAME);
        shipment.setProviderCode(PROVIDER_CODE);
        shipment.setTrackingNumber(normalizedAwb);

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        String eventKey = "ADMIN|AWB_ASSIGNED|" + saved.getId() + "|" + normalizedAwb;
        if (!trackingEventRepository.existsByEventKey(eventKey)) {
            ShipmentTrackingEvent event = new ShipmentTrackingEvent();
            event.setShipment(saved);
            event.setEventKey(eventKey);
            event.setExternalStatus("created");
            event.setExternalStatusDisplay("Shipment created");
            event.setShipmentStatus("CREATED");
            event.setRemarks("Shadowfax AWB linked to this order by admin.");
            event.setEventAt(java.time.LocalDateTime.now());
            event.setSource("ADMIN");
            trackingEventRepository.save(event);
        }

        return saved;
    }

    /**
     * Saves the manually supplied AWB first, then attempts an immediate
     * Shadowfax tracking lookup. If Shadowfax has not exposed the shipment
     * yet, the AWB remains saved locally and the normal scheduler/webhook
     * flow can synchronize it later.
     */
    public Shipment assignManualShadowfaxAwbAndSync(Long orderId, String awb) {
        Shipment saved = assignManualShadowfaxAwb(orderId, awb);
        try {
            return shadowfaxTrackingService.syncShipmentTracking(saved.getId());
        } catch (RuntimeException ignored) {
            return shipmentRepository.findById(saved.getId()).orElse(saved);
        }
    }

    @Transactional
    public Shipment createShipmentForOrder(Long orderId) {

        if (orderId == null) {
            throw new ApiException(
                    "ORDER_ID_REQUIRED",
                    "Order ID is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Lock the order row while this shipment creation is running.
         *
         * This prevents two simultaneous requests from both creating
         * a Shadowfax shipment for the same order.
         */
        Order order =
                orderRepository.findByIdForUpdate(orderId)
                        .orElseThrow(() ->
                                new ApiException(
                                        "ORDER_NOT_FOUND",
                                        "Order not found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        /*
         * Never create another external shipment if one already
         * exists locally.
         */
        if (shipmentRepository
                .findByOrderId(orderId)
                .isPresent()) {

            throw new ApiException(
                    "SHIPMENT_ALREADY_EXISTS",
                    "Shipment already exists for this order",
                    HttpStatus.CONFLICT
            );
        }

        validateConfiguration();

        validateOrderForShipment(order);

        if (order.getItems() == null
                || order.getItems().isEmpty()) {

            throw new ApiException(
                    "ORDER_HAS_NO_ITEMS",
                    "Order has no items",
                    HttpStatus.BAD_REQUEST
            );
        }

        BigDecimal productValue = BigDecimal.ZERO;

        List<ShadowfaxClient.ProductDetails> productDetails =
                new ArrayList<>();

        for (OrderItem item : order.getItems()) {

            validateOrderItem(item);

            BigDecimal quantity =
                    BigDecimal.valueOf(item.getQuantity() * Math.max(1, item.getPackSize()));

            BigDecimal itemValue =
                    item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

            /*
             * Shadowfax product_value is the total SKU value
             * excluding tax.
             */
            productValue =
                    productValue.add(itemValue);

            /*
             * The Shadowfax API documentation makes the
             * product-level tax object optional.
             *
             * We deliberately do not invent a CGST/SGST split
             * because our order item currently stores only one
             * aggregate GST amount.
             */
            ShadowfaxClient.Taxes taxes = null;

            productDetails.add(
                    new ShadowfaxClient.ProductDetails(

                            null,

                            order.getOrderNumber(),

                            item.getProductName(),

                            item.getProductSku(),

                            "GENERAL",

                            /*
                             * Shadowfax product price is the
                             * product/SKU price.
                             *
                             * Quantity is sent separately below.
                             */
                            item.getUnitPrice(),

                            null,

                            taxes,

                            new ShadowfaxClient.AdditionalDetails(
                                    "False",
                                    null,
                                    item.getQuantity() * Math.max(1, item.getPackSize())
                            )
                    )
            );
        }

        /*
         * Determine Shadowfax payment mode from the explicit
         * order payment method.
         *
         * We NEVER infer COD/Prepaid from payment_status alone.
         */
        String paymentMode =
                determinePaymentMode(order);

        BigDecimal codAmount =
                "COD".equals(paymentMode)
                        ? order.getTotal()
                        : BigDecimal.ZERO;

        ShadowfaxClient.OrderDetails orderDetails =
                new ShadowfaxClient.OrderDetails(

                        /*
                         * Stable order identifier from our system.
                         */
                        order.getOrderNumber(),

                        /*
                         * AWB intentionally omitted.
                         *
                         * Shadowfax will assign the AWB.
                         */
                        null,

                        /*
                         * Weight is not currently stored in our
                         * order model.
                         *
                         * Shadowfax documentation allows these
                         * fields to be omitted/defaulted.
                         */
                        null,

                        null,

                        productValue,

                        codAmount,

                        paymentMode,

                        null,

                        order.getTotal(),

                        null,

                        null,

                        "regular"
                );

        ShadowfaxClient.CustomerDetails customerDetails =
                new ShadowfaxClient.CustomerDetails(

                        order.getAddressName(),

                        order.getAddressPhone(),

                        null,

                        order.getAddressLine1(),

                        order.getAddressLine2(),

                        order.getAddressCity(),

                        order.getAddressState(),

                        parsePincode(
                                order.getAddressPincode(),
                                "CUSTOMER_ADDRESS"
                        ),

                        null,

                        null
                );

        ShadowfaxClient.PickupDetails pickupDetails =
                new ShadowfaxClient.PickupDetails(

                        properties.getPickupName(),

                        properties.getPickupContact(),

                        properties.getPickupAddressLine1(),

                        properties.getPickupAddressLine2(),

                        properties.getPickupCity(),

                        properties.getPickupState(),

                        parsePincode(
                                properties.getPickupPincode(),
                                "SHADOWFAX_PICKUP"
                        ),

                        null,

                        null,

                        blankToNull(
                                properties.getPickupUniqueCode()
                        )
                );

        ShadowfaxClient.RtsDetails rtsDetails =
                new ShadowfaxClient.RtsDetails(

                        properties.getRtsName(),

                        properties.getRtsContact(),

                        properties.getRtsAddressLine1(),

                        properties.getRtsAddressLine2(),

                        properties.getRtsCity(),

                        properties.getRtsState(),

                        parsePincode(
                                properties.getRtsPincode(),
                                "SHADOWFAX_RTS"
                        ),

                        properties.getRtsEmail(),

                        null,

                        null,

                        blankToNull(
                                properties.getRtsUniqueCode()
                        )
                );

        ShadowfaxClient.ShadowfaxOrderRequest request =
                new ShadowfaxClient.ShadowfaxOrderRequest(

                        "marketplace",

                        orderDetails,

                        customerDetails,

                        pickupDetails,

                        rtsDetails,

                        productDetails
                );

        ShadowfaxClient.ShadowfaxOrderResponse response =
                shadowfaxClient.createMarketplaceOrder(request);

        validateShadowfaxResponse(response);

        ShadowfaxClient.ShadowfaxOrderData data =
                response.data();

        String awbNumber =
                data.awb_number();

        if (awbNumber == null
                || awbNumber.isBlank()) {

            throw new ApiException(
                    "SHADOWFAX_AWB_NOT_RETURNED",
                    "Shadowfax accepted the order but did not return an AWB",
                    HttpStatus.BAD_GATEWAY
            );
        }

        Shipment shipment = new Shipment();

        shipment.setOrder(order);
        shipment.setCourierName(COURIER_NAME);
        shipment.setProviderCode(PROVIDER_CODE);

        shipment.setTrackingNumber(
                awbNumber.trim()
        );

        /*
         * Shadowfax's internal shipment/order ID.
         */
        if (data.id() != null) {
            shipment.setExternalShipmentId(
                    String.valueOf(data.id())
            );
        }

        /*
         * Preserve Shadowfax's original status separately
         * from our internal shipment status.
         */
        shipment.setExternalStatus(
                blankToNull(data.status())
        );

        shipment.setExternalStatusDisplay(
                blankToNull(data.status_display())
        );

        shipment.setShipmentStatus(
                mapShadowfaxStatus(data.status())
        );

        if ("PICKED_UP".equals(
                shipment.getShipmentStatus()
        )) {
            shipment.setShippedAt(
                    java.time.LocalDateTime.now()
            );
        }

        if ("DELIVERED".equals(
                shipment.getShipmentStatus()
        )) {
            java.time.LocalDateTime now =
                    java.time.LocalDateTime.now();

            shipment.setShippedAt(now);
            shipment.setDeliveredAt(now);

            // Keep the existing order synchronized with the shipment delivery event.
            if (order != null) {
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(shipment.getDeliveredAt());
                }
                order.setOrderStatus("DELIVERED");
                orderRepository.save(order);
            }
        }

        shipment.setLastSyncedAt(
                java.time.LocalDateTime.now()
        );

        return shipmentRepository.save(shipment);
    }

    private void validateOrderForShipment(
            Order order
    ) {

        String paymentMethod =
                order.getPaymentMethod();

        if (paymentMethod == null
                || paymentMethod.isBlank()) {

            throw new ApiException(
                    "PAYMENT_METHOD_REQUIRED",
                    "Order payment method is required before shipment creation",
                    HttpStatus.CONFLICT
            );
        }

        String normalizedPaymentMethod =
                paymentMethod
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String paymentStatus =
                order.getPaymentStatus();

        if (paymentStatus == null
                || paymentStatus.isBlank()) {

            throw new ApiException(
                    "PAYMENT_STATUS_REQUIRED",
                    "Order payment status is required before shipment creation",
                    HttpStatus.CONFLICT
            );
        }

        String normalizedPaymentStatus =
                paymentStatus
                        .trim()
                        .toUpperCase(Locale.ROOT);

        /*
         * COD:
         *
         * The customer pays the courier at delivery.
         * Therefore payment_status can remain PENDING.
         */
        if ("COD".equals(normalizedPaymentMethod)) {

            if (!"PENDING".equals(normalizedPaymentStatus)
                    && !"COD".equals(normalizedPaymentStatus)) {

                throw new ApiException(
                        "INVALID_COD_PAYMENT_STATE",
                        "COD order is not in a valid payment state",
                        HttpStatus.CONFLICT
                );
            }
        }

        /*
         * Razorpay:
         *
         * Shipment creation is allowed only after the payment
         * has actually been verified and the order is PAID.
         */
        else if ("CASHFREE".equals(normalizedPaymentMethod) || "RAZORPAY".equals(normalizedPaymentMethod)) {

            if (!"PAID".equals(normalizedPaymentStatus)) {

                throw new ApiException(
                        "PAYMENT_NOT_COMPLETED",
                        "Razorpay payment has not been completed",
                        HttpStatus.CONFLICT
                );
            }
        }

        else {

            throw new ApiException(
                    "INVALID_PAYMENT_METHOD",
                    "Unsupported payment method: "
                            + paymentMethod,
                    HttpStatus.BAD_REQUEST
            );
        }

        String orderStatus =
                order.getOrderStatus();

        if (orderStatus != null) {

            String normalizedOrderStatus =
                    orderStatus
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if ("CANCELLED".equals(normalizedOrderStatus)
                    || "CANCELED".equals(normalizedOrderStatus)
                    || "DELIVERED".equals(normalizedOrderStatus)) {

                throw new ApiException(
                        "ORDER_NOT_ELIGIBLE_FOR_SHIPMENT",
                        "Order is not eligible for shipment creation",
                        HttpStatus.CONFLICT
                );
            }
        }
    }

    private String determinePaymentMode(
            Order order
    ) {

        String paymentMethod =
                order.getPaymentMethod();

        if (paymentMethod == null
                || paymentMethod.isBlank()) {

            throw new ApiException(
                    "PAYMENT_METHOD_REQUIRED",
                    "Payment method is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        String normalizedPaymentMethod =
                paymentMethod
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if ("COD".equals(normalizedPaymentMethod)) {

            /*
             * COD means Shadowfax must collect the order total
             * from the customer.
             */
            return "COD";
        }

        if ("CASHFREE".equals(normalizedPaymentMethod) || "RAZORPAY".equals(normalizedPaymentMethod)) {

            /*
             * validateOrderForShipment() has already confirmed
             * that the Razorpay order is PAID.
             */
            return "Prepaid";
        }

        throw new ApiException(
                "INVALID_PAYMENT_METHOD",
                "Unsupported payment method: "
                        + paymentMethod,
                HttpStatus.BAD_REQUEST
        );
    }

    private void validateOrderItem(
            OrderItem item
    ) {

        if (item == null) {

            throw new ApiException(
                    "INVALID_ORDER_ITEM",
                    "Order contains an invalid item",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (item.getUnitPrice() == null
                || item.getUnitPrice().signum() < 0) {

            throw new ApiException(
                    "INVALID_ORDER_ITEM_PRICE",
                    "Order contains an invalid item price",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (item.getQuantity() <= 0) {

            throw new ApiException(
                    "INVALID_ORDER_ITEM_QUANTITY",
                    "Order contains an invalid item quantity",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (isBlank(item.getProductName())) {

            throw new ApiException(
                    "INVALID_ORDER_ITEM_NAME",
                    "Order item product name is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (isBlank(item.getProductSku())) {

            throw new ApiException(
                    "INVALID_ORDER_ITEM_SKU",
                    "Order item SKU is required",
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateShadowfaxResponse(
            ShadowfaxClient.ShadowfaxOrderResponse response
    ) {

        if (response == null) {

            throw new ApiException(
                    "SHADOWFAX_EMPTY_RESPONSE",
                    "Shadowfax returned an empty response",
                    HttpStatus.BAD_GATEWAY
            );
        }

        if (!"Success".equalsIgnoreCase(
                response.message()
        )) {

            String errorMessage =
                    response.errors() == null
                            ? "Shadowfax order creation failed"
                            : response.errors().toString();

            throw new ApiException(
                    "SHADOWFAX_ORDER_FAILED",
                    errorMessage,
                    HttpStatus.BAD_GATEWAY
            );
        }

        if (response.data() == null) {

            throw new ApiException(
                    "SHADOWFAX_ORDER_DATA_MISSING",
                    "Shadowfax accepted the request but returned no order data",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private void validateConfiguration() {

        if (isBlank(properties.getPickupName())
                || isBlank(properties.getPickupContact())
                || isBlank(properties.getPickupAddressLine1())
                || isBlank(properties.getPickupCity())
                || isBlank(properties.getPickupState())
                || isBlank(properties.getPickupPincode())) {

            throw new ApiException(
                    "SHADOWFAX_PICKUP_CONFIGURATION_INVALID",
                    "Shadowfax pickup configuration is incomplete",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (isBlank(properties.getRtsName())
                || isBlank(properties.getRtsContact())
                || isBlank(properties.getRtsAddressLine1())
                || isBlank(properties.getRtsCity())
                || isBlank(properties.getRtsState())
                || isBlank(properties.getRtsPincode())) {

            throw new ApiException(
                    "SHADOWFAX_RTS_CONFIGURATION_INVALID",
                    "Shadowfax RTS configuration is incomplete",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private Integer parsePincode(
            String pincode,
            String fieldName
    ) {

        if (pincode == null
                || pincode.isBlank()) {

            throw new ApiException(
                    "INVALID_PINCODE",
                    fieldName + " pincode is required",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        try {

            int parsed =
                    Integer.parseInt(pincode.trim());

            if (parsed < 100000
                    || parsed > 999999) {

                throw new NumberFormatException();
            }

            return parsed;

        } catch (NumberFormatException e) {

            throw new ApiException(
                    "INVALID_PINCODE",
                    fieldName
                            + " pincode must be a valid 6 digit number",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String mapShadowfaxStatus(
            String shadowfaxStatus
    ) {

        if (shadowfaxStatus == null
                || shadowfaxStatus.isBlank()) {

            return "CREATED";
        }

        return switch (
                shadowfaxStatus
                        .trim()
                        .toLowerCase(Locale.ROOT)
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

    private String blankToNull(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.isBlank();
    }
}