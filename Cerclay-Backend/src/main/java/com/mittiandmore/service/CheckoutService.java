package com.mittiandmore.service;

import com.mittiandmore.dto.CheckoutSummaryRequest;
import com.mittiandmore.dto.CheckoutSummaryResponse;
import com.mittiandmore.dto.ShippingQuoteRequest;
import com.mittiandmore.dto.ShippingQuoteResponse;
import com.mittiandmore.entity.Address;
import com.mittiandmore.entity.Cart;
import com.mittiandmore.entity.CartItem;
import com.mittiandmore.entity.Customer;
import com.mittiandmore.entity.Product;
import com.mittiandmore.entity.StoreSettings;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.AddressRepository;
import com.mittiandmore.repository.CartRepository;
import com.mittiandmore.repository.CustomerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final CustomerRepository customers;
    private final CartRepository carts;
    private final AddressRepository addresses;
    private final DiscountService discounts;
    private final StoreSettingsService settingsService;
    private final ShippingService shippingService;

    public CheckoutService(
        CustomerRepository customers,
        CartRepository carts,
        AddressRepository addresses,
        DiscountService discounts,
        StoreSettingsService settingsService,
        ShippingService shippingService
    ) {
        this.customers = customers;
        this.carts = carts;
        this.addresses = addresses;
        this.discounts = discounts;
        this.settingsService = settingsService;
        this.shippingService = shippingService;
    }

    @Transactional(readOnly = true)
    public CheckoutSummaryResponse summary(Long customerId, CheckoutSummaryRequest request) {
        Customer customer = customers
            .findById(customerId)
            .orElseThrow(() -> new ApiException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        Cart cart = carts
            .findByCustomerId(customer.getId())
            .orElseThrow(() -> new ApiException("CART_NOT_FOUND", "Customer cart not found", HttpStatus.NOT_FOUND));
        if (cart.getItems() == null || cart.getItems().isEmpty()) throw new ApiException(
            "CART_EMPTY",
            "Cannot calculate checkout for an empty cart",
            HttpStatus.BAD_REQUEST
        );
        Address address = addresses
            .findById(request.getAddressId())
            .orElseThrow(() -> new ApiException("ADDRESS_NOT_FOUND", "Address not found", HttpStatus.NOT_FOUND));
        if (!address.getCustomer().getId().equals(customer.getId())) throw new ApiException(
            "ADDRESS_ACCESS_DENIED",
            "You are not allowed to use this address",
            HttpStatus.FORBIDDEN
        );
        String method =
            request.getPaymentMethod() == null
                ? "CASHFREE"
                : request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!method.equals("COD") && !method.equals("CASHFREE") && !method.equals("RAZORPAY")) throw new ApiException(
            "INVALID_PAYMENT_METHOD",
            "Payment method must be COD, CASHFREE or RAZORPAY",
            HttpStatus.BAD_REQUEST
        );

        StoreSettings settings = settingsService.getSettings();
        BigDecimal subtotal = BigDecimal.ZERO,
            productDiscount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product == null || !Boolean.TRUE.equals(product.getActive())) throw new ApiException(
                "PRODUCT_UNAVAILABLE",
                "A product in your cart is no longer available",
                HttpStatus.BAD_REQUEST
            );
            if (item.getQuantity() <= 0 || product.getStock() < item.getQuantity()) throw new ApiException(
                "INSUFFICIENT_STOCK",
                "Insufficient stock for " + product.getName(),
                HttpStatus.BAD_REQUEST
            );
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            subtotal = subtotal.add(product.getPrice().multiply(qty));
            productDiscount = productDiscount.add(
                product.getMrp().subtract(product.getPrice()).max(BigDecimal.ZERO).multiply(qty)
            );
        }
        subtotal = money(subtotal);
        productDiscount = money(productDiscount);
        if (subtotal.compareTo(settings.getMinimumOrderValue()) < 0) throw new ApiException(
            "MINIMUM_ORDER_VALUE_NOT_MET",
            "Minimum order value is ₹" + money(settings.getMinimumOrderValue()),
            HttpStatus.BAD_REQUEST
        );
        BigDecimal couponDiscount = money(discounts.calculate(request.getCouponCode(), subtotal, customer.getId()));
        // Product prices are customer-facing tax-inclusive prices. GST is not added again at checkout.
        BigDecimal gst = BigDecimal.ZERO.setScale(2);
        ShippingQuoteRequest shippingRequest = new ShippingQuoteRequest();
        shippingRequest.setPincode(address.getPincode());
        shippingRequest.setOrderValue(subtotal);
        shippingRequest.setPaymentMethod(method);
        ShippingQuoteResponse shipping = shippingService.calculateQuote(shippingRequest);
        if (!shipping.isServiceable()) throw new ApiException(
            "DELIVERY_NOT_AVAILABLE",
            shipping.getMessage(),
            HttpStatus.BAD_REQUEST
        );
        BigDecimal shippingCharge = money(shipping.getCustomerShippingCharge());
        BigDecimal total = money(subtotal.subtract(couponDiscount).add(shippingCharge));
        CheckoutSummaryResponse response = new CheckoutSummaryResponse();
        response.setSubtotal(subtotal);
        response.setProductDiscount(productDiscount);
        response.setCouponDiscount(couponDiscount);
        response.setGst(gst);
        response.setShippingCharge(shippingCharge);
        response.setTotal(total);
        response.setPaymentMethod(method);
        response.setCouponCode(
            request.getCouponCode() == null ? null : request.getCouponCode().trim().toUpperCase(Locale.ROOT)
        );
        response.setServiceable(true);
        response.setShippingZone(shipping.getZoneCode());
        return response;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }
}
