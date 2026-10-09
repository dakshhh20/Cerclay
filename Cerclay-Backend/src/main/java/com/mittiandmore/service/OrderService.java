package com.mittiandmore.service;

import com.mittiandmore.dto.CancelOrderRequest;
import com.mittiandmore.dto.OrderCreateRequest;
import com.mittiandmore.dto.OrderItemResponse;
import com.mittiandmore.dto.OrderResponse;
import com.mittiandmore.dto.ShippingQuoteRequest;
import com.mittiandmore.dto.ShippingQuoteResponse;
import com.mittiandmore.entity.Address;
import com.mittiandmore.entity.Cart;
import com.mittiandmore.entity.CartItem;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Order;
import com.mittiandmore.entity.OrderItem;
import com.mittiandmore.entity.Payment;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.StoreSettings;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.notification.NotificationEventType;
import com.mittiandmore.notification.NotificationService;
import com.mittiandmore.repository.AddressRepository;
import com.mittiandmore.repository.CartRepository;
import com.mittiandmore.repository.CustomerRepository;
import com.mittiandmore.repository.OrderRepository;
import com.mittiandmore.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final DiscountService discountService;
    private final StoreSettingsService storeSettingsService;
    private final ShippingService shippingService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    public OrderService(
        OrderRepository orderRepository,
        CustomerRepository customerRepository,
        CartRepository cartRepository,
        AddressRepository addressRepository,
        DiscountService discountService,
        StoreSettingsService storeSettingsService,
        ShippingService shippingService,
        InventoryService inventoryService,
        NotificationService notificationService,
        PaymentRepository paymentRepository,
        RefundService refundService
    ) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.discountService = discountService;
        this.storeSettingsService = storeSettingsService;
        this.shippingService = shippingService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.paymentRepository = paymentRepository;
        this.refundService = refundService;
    }

    @Transactional
    public OrderResponse createOrder(Long customerId, OrderCreateRequest request) {
        Customer customer = customerRepository
            .findById(customerId)
            .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));

        Cart cart = cartRepository
            .findByCustomerId(customerId)
            .orElseThrow(() -> new ApiException("CART_NOT_FOUND", "Customer cart not found", HttpStatus.NOT_FOUND));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ApiException("CART_EMPTY", "Cannot create order from an empty cart", HttpStatus.BAD_REQUEST);
        }

        Address address = addressRepository
            .findById(request.getAddressId())
            .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found", HttpStatus.NOT_FOUND));

        if (!address.getCustomer().getId().equals(customerId)) {
            throw new ApiException(
                "ADDRESS_ACCESS_DENIED",
                "You are not allowed to use this address",
                HttpStatus.FORBIDDEN
            );
        }

        /*
         * Explicitly validate and normalize the payment method.
         *
         * Supported payment methods:
         *
         * COD
         * RAZORPAY
         *
         * We do not infer the payment method from payment_status.
         */
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new ApiException("PAYMENT_METHOD_REQUIRED", "Payment method is required", HttpStatus.BAD_REQUEST);
        }

        String paymentMethod = request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        if (!"COD".equals(paymentMethod) && !"CASHFREE".equals(paymentMethod) && !"RAZORPAY".equals(paymentMethod)) {
            throw new ApiException(
                "INVALID_PAYMENT_METHOD",
                "Payment method must be COD, CASHFREE or RAZORPAY",
                HttpStatus.BAD_REQUEST
            );
        }

        StoreSettings settings = storeSettingsService.getSettings();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product == null) {
                throw new ApiException("INVALID_CART_ITEM", "Cart contains an invalid product", HttpStatus.BAD_REQUEST);
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new ApiException(
                    "PRODUCT_UNAVAILABLE",
                    "Product is no longer available: " + product.getName(),
                    HttpStatus.BAD_REQUEST
                );
            }

            int quantity = cartItem.getQuantity();
            int packSize = Math.max(1, cartItem.getPackSize());
            int units = quantity * packSize;

            if (quantity <= 0) {
                throw new ApiException("INVALID_QUANTITY", "Cart contains an invalid quantity", HttpStatus.BAD_REQUEST);
            }

            /*
             * Check that enough stock exists before creating
             * the order item.
             */
            if (product.getStock() < units) {
                throw new ApiException(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock for product: " + product.getName(),
                    HttpStatus.BAD_REQUEST
                );
            }

            BigDecimal unitPrice = packSize == 2 ? product.getSetOf2Price() : product.getPrice();
            BigDecimal unitMrp = packSize == 2 ? product.getMrp().multiply(BigDecimal.valueOf(2)) : product.getMrp();

            if (
                packSize == 2 && (!Boolean.TRUE.equals(product.getSetOf2Enabled()) || product.getSetOf2Price() == null)
            ) {
                throw new ApiException(
                    "SET_OF_2_NOT_AVAILABLE",
                    "Set of 2 is not available for product: " + product.getName(),
                    HttpStatus.BAD_REQUEST
                );
            }

            if (unitPrice == null || unitMrp == null) {
                throw new ApiException(
                    "INVALID_PRODUCT_PRICE",
                    "Product has invalid pricing: " + product.getName(),
                    HttpStatus.BAD_REQUEST
                );
            }

            if (unitPrice.compareTo(BigDecimal.ZERO) < 0 || unitMrp.compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(
                    "INVALID_PRODUCT_PRICE",
                    "Product has invalid pricing: " + product.getName(),
                    HttpStatus.BAD_REQUEST
                );
            }

            BigDecimal quantityDecimal = BigDecimal.valueOf(quantity);

            BigDecimal itemSellingPrice = unitPrice.multiply(quantityDecimal);

            BigDecimal itemMrp = unitMrp.multiply(quantityDecimal);

            BigDecimal itemDiscount = itemMrp.subtract(itemSellingPrice).max(BigDecimal.ZERO);

            subtotal = subtotal.add(itemSellingPrice);

            discount = discount.add(itemDiscount);

            /*
             * Reduce stock as part of the same transaction.
             *
             * Product has @Version, so Hibernate will include
             * the version in the UPDATE statement.
             *
             * If another transaction changed this product first,
             * Hibernate will detect the version conflict and the
             * transaction will fail instead of silently overwriting
             * the other stock update.
             */
            int previousStock = product.getStock();
            product.setStock(previousStock - units);

            inventoryService.recordStockChange(
                product,
                previousStock,
                product.getStock(),
                "ORDER_PLACED",
                "Stock reserved for order",
                "SYSTEM"
            );

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setProductName(packSize == 2 ? product.getName() + " · Set of 2" : product.getName());
            orderItem.setProductSku(packSize == 2 ? product.getSku() + "-SET2" : product.getSku());
            orderItem.setQuantity(quantity);
            orderItem.setPackSize(packSize);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setUnitMrp(unitMrp);
            orderItem.setDiscount(scaleMoney(itemDiscount));

            orderItems.add(orderItem);
        }

        subtotal = scaleMoney(subtotal);
        discount = scaleMoney(discount);

        BigDecimal couponDiscount = discountService.calculateAndConsume(request.getCouponCode(), subtotal, customerId);
        discount = scaleMoney(discount.add(couponDiscount));

        if (subtotal.compareTo(settings.getMinimumOrderValue()) < 0) {
            throw new ApiException(
                "MINIMUM_ORDER_VALUE_NOT_MET",
                "Minimum order value is ₹" + scaleMoney(settings.getMinimumOrderValue()),
                HttpStatus.BAD_REQUEST
            );
        }

        // Product prices are customer-facing tax-inclusive prices. GST is not added again to the order total.
        BigDecimal gst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        ShippingQuoteRequest shippingRequest = new ShippingQuoteRequest();
        shippingRequest.setPincode(address.getPincode());
        shippingRequest.setOrderValue(subtotal);
        shippingRequest.setPaymentMethod(paymentMethod);

        ShippingQuoteResponse shippingQuote = shippingService.calculateQuote(shippingRequest);

        if (!shippingQuote.isServiceable()) {
            throw new ApiException("DELIVERY_NOT_AVAILABLE", shippingQuote.getMessage(), HttpStatus.BAD_REQUEST);
        }

        BigDecimal shippingCharge = scaleMoney(shippingQuote.getCustomerShippingCharge());

        BigDecimal total = subtotal.subtract(couponDiscount).add(gst).add(shippingCharge);

        total = scaleMoney(total);

        for (OrderItem orderItem : orderItems) {
            BigDecimal itemSellingTotal = orderItem
                .getUnitPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

            // Item selling prices already include taxes. Keep GST stored as zero for schema/backward compatibility.
            BigDecimal itemGst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal itemTotal = itemSellingTotal;

            orderItem.setGst(scaleMoney(itemGst));

            orderItem.setTotal(scaleMoney(itemTotal));
        }

        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());

        order.setCustomer(customer);

        order.setSubtotal(subtotal);

        order.setDiscount(discount);
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            order.setCouponCode(request.getCouponCode().trim().toUpperCase(Locale.ROOT));
        }

        order.setGst(gst);

        order.setShippingCharge(shippingCharge);

        order.setTotal(total);

        /*
         * Store an address snapshot inside the order.
         * Future changes to the customer's saved address
         * will not change the historical order.
         */
        order.setAddressName(address.getName());
        order.setAddressPhone(address.getPhone());
        order.setAddressLine1(address.getHouse());
        order.setAddressLine2(address.getStreet());
        order.setAddressCity(address.getCity());
        order.setAddressState(address.getState());
        order.setAddressPincode(address.getPincode());
        order.setAddressType(address.getAddressType());

        /*
         * Store the payment method explicitly.
         *
         * COD:
         * payment_method = COD
         * payment_status = PENDING
         *
         * Razorpay:
         * payment_method = RAZORPAY
         * payment_status = PENDING initially
         *
         * The Razorpay payment flow will later change the
         * payment status to PAID only after successful
         * payment verification.
         */
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PLACED");

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
        }

        order.setItems(orderItems);

        /*
         * Both the order and the stock changes are committed
         * together because this method is @Transactional.
         */
        Order savedOrder = orderRepository.save(order);

        notificationService.enqueue(NotificationEventType.ORDER_PLACED, savedOrder, null, null);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        Order order = findOrder(id);
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        Set<String> allowed = Set.of("PLACED", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED");
        if (!allowed.contains(normalized)) {
            throw new ApiException("INVALID_ORDER_STATUS", "Unsupported order status", HttpStatus.BAD_REQUEST);
        }
        String previousStatus = order.getOrderStatus();
        order.setOrderStatus(normalized);
        if ("DELIVERED".equals(normalized) && !"DELIVERED".equals(previousStatus) && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        Order saved = orderRepository.save(order);

        if (!normalized.equals(previousStatus)) {
            if ("SHIPPED".equals(normalized)) {
                notificationService.enqueue(NotificationEventType.ORDER_SHIPPED, saved, null, null);
            } else if ("DELIVERED".equals(normalized)) {
                notificationService.enqueue(NotificationEventType.ORDER_DELIVERED, saved, null, null);
            } else if ("CANCELLED".equals(normalized)) {
                throw new ApiException(
                    "CANCELLATION_REQUIRES_CANCEL_ENDPOINT",
                    "Use the cancellation endpoint so inventory and payment rules are applied",
                    HttpStatus.BAD_REQUEST
                );
            }
        }

        return mapToResponse(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, String reason, String cancelledBy) {
        Order order = orderRepository
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        String currentStatus = order.getOrderStatus();
        if ("CANCELLED".equals(currentStatus)) {
            throw new ApiException("ORDER_ALREADY_CANCELLED", "Order is already cancelled", HttpStatus.CONFLICT);
        }

        if (Set.of("SHIPPED", "DELIVERED").contains(currentStatus)) {
            throw new ApiException(
                "ORDER_CANNOT_BE_CANCELLED",
                "Shipped or delivered orders cannot be cancelled through this flow",
                HttpStatus.CONFLICT
            );
        }

        if (!Set.of("PLACED", "PROCESSING", "PACKED").contains(currentStatus)) {
            throw new ApiException(
                "ORDER_CANNOT_BE_CANCELLED",
                "Order cannot be cancelled in its current status",
                HttpStatus.CONFLICT
            );
        }

        // Paid Razorpay orders are now cancelled only as part of the real refund workflow.
        if (
            ("CASHFREE".equalsIgnoreCase(order.getPaymentMethod()) ||
                "RAZORPAY".equalsIgnoreCase(order.getPaymentMethod())) &&
            "PAID".equalsIgnoreCase(order.getPaymentStatus())
        ) {
            refundService.refundCancelledOrder(order, reason);
        }

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                inventoryService.restoreStockForCancellation(
                    item.getProduct().getId(),
                    item.getQuantity(),
                    order.getId()
                );
            }
        }

        if (
            "CASHFREE".equalsIgnoreCase(order.getPaymentMethod()) ||
            "RAZORPAY".equalsIgnoreCase(order.getPaymentMethod())
        ) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (payment != null && !"PAID".equalsIgnoreCase(payment.getPaymentStatus())) {
                payment.setPaymentStatus("CANCELLED");
                paymentRepository.save(payment);
            }
        }

        discountService.releaseUsage(order.getCouponCode());

        order.setOrderStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason == null || reason.isBlank() ? "Order cancelled" : reason.trim());
        order.setCancelledBy(cancelledBy == null || cancelledBy.isBlank() ? "SYSTEM" : cancelledBy);

        Order saved = orderRepository.save(order);

        notificationService.enqueue(NotificationEventType.ORDER_CANCELLED, saved, null, null);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = findOrder(id);

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository
            .findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll(
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }

        return responses;
    }

    private Order findOrder(Long id) {
        return orderRepository
            .findById(id)
            .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getCustomer().getId());

        response.setSubtotal(order.getSubtotal());

        response.setDiscount(order.getDiscount());
        response.setCouponCode(order.getCouponCode());

        response.setGst(order.getGst());

        response.setShippingCharge(order.getShippingCharge());

        response.setTotal(order.getTotal());

        response.setAddressName(order.getAddressName());

        response.setAddressPhone(order.getAddressPhone());

        response.setAddressLine1(order.getAddressLine1());

        response.setAddressLine2(order.getAddressLine2());

        response.setAddressCity(order.getAddressCity());

        response.setAddressState(order.getAddressState());

        response.setAddressPincode(order.getAddressPincode());

        response.setAddressType(order.getAddressType());

        /*
         * Return the explicit payment method to the frontend.
         */
        response.setPaymentMethod(order.getPaymentMethod());

        response.setPaymentStatus(order.getPaymentStatus());

        response.setOrderStatus(order.getOrderStatus());

        response.setCreatedAt(order.getCreatedAt());

        response.setUpdatedAt(order.getUpdatedAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setCancelledAt(order.getCancelledAt());
        response.setCancellationReason(order.getCancellationReason());
        response.setCancelledBy(order.getCancelledBy());

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();

                itemResponse.setId(item.getId());

                itemResponse.setProductId(item.getProduct().getId());

                itemResponse.setProductName(item.getProductName());

                itemResponse.setProductSku(item.getProductSku());

                itemResponse.setQuantity(item.getQuantity());
                itemResponse.setPackSize(item.getPackSize());

                itemResponse.setUnitPrice(item.getUnitPrice());

                itemResponse.setUnitMrp(item.getUnitMrp());

                itemResponse.setDiscount(item.getDiscount());

                itemResponse.setGst(item.getGst());

                itemResponse.setTotal(item.getTotal());

                itemResponses.add(itemResponse);
            }
        }

        response.setItems(itemResponses);

        return response;
    }

    private BigDecimal calculateGst(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + timestamp + "-" + randomPart;
    }
}
