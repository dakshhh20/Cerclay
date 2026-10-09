package com.mittiandmore.service;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ShadowfaxClient {

    private static final Logger log = LoggerFactory.getLogger(ShadowfaxClient.class);

    private final RestClient shadowfaxRestClient;

    public ShadowfaxClient(RestClient shadowfaxRestClient) {
        this.shadowfaxRestClient = shadowfaxRestClient;
    }

    public List<ServiceabilityResponse> checkCustomerDeliveryServiceability(String pincode) {
        if (pincode == null || pincode.isBlank()) {
            throw new IllegalArgumentException("Pincode is required");
        }

        log.info("Checking Shadowfax customer delivery serviceability for pincode={}", pincode);

        return shadowfaxRestClient
            .get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/v1/clients/serviceability/")
                    .queryParam("service", "customer_delivery")
                    .queryParam("page", 1)
                    .queryParam("count", 10)
                    .queryParam("pincodes", pincode)
                    .build()
            )
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                log.error("Shadowfax serviceability request failed with status={}", response.getStatusCode());

                throw new IllegalStateException("Shadowfax serviceability request failed: " + response.getStatusCode());
            })
            .body(new ParameterizedTypeReference<List<ServiceabilityResponse>>() {});
    }

    public AwbGenerationResponse generateAwbs(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("AWB count must be greater than 0");
        }

        if (count > 100000) {
            throw new IllegalArgumentException("AWB count cannot exceed 100000");
        }

        AwbGenerationRequest request = new AwbGenerationRequest(count);

        log.info("Requesting {} Shadowfax marketplace AWB(s)", count);

        return shadowfaxRestClient
            .post()
            .uri("/v3/clients/generate_marketplace_awb/")
            .body(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                log.error("Shadowfax AWB generation request failed with status={}", response.getStatusCode());

                throw new IllegalStateException("Shadowfax AWB generation request failed: " + response.getStatusCode());
            })
            .body(AwbGenerationResponse.class);
    }

    public ShadowfaxOrderResponse createMarketplaceOrder(ShadowfaxOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Shadowfax order request is required");
        }

        log.info(
            "Creating Shadowfax marketplace order: " +
                "clientOrderId={}, paymentMode={}, " +
                "pickupPincode={}, pickupCity={}, pickupState={}, " +
                "pickupUniqueCode={}, customerPincode={}, " +
                "customerCity={}, customerState={}, " +
                "productValue={}, codAmount={}, totalAmount={}, " +
                "productCount={}",
            request.order_details() != null ? request.order_details().client_order_id() : null,
            request.order_details() != null ? request.order_details().payment_mode() : null,
            request.pickup_details() != null ? request.pickup_details().pincode() : null,
            request.pickup_details() != null ? request.pickup_details().city() : null,
            request.pickup_details() != null ? request.pickup_details().state() : null,
            request.pickup_details() != null ? request.pickup_details().unique_code() : null,
            request.customer_details() != null ? request.customer_details().pincode() : null,
            request.customer_details() != null ? request.customer_details().city() : null,
            request.customer_details() != null ? request.customer_details().state() : null,
            request.order_details() != null ? request.order_details().product_value() : null,
            request.order_details() != null ? request.order_details().cod_amount() : null,
            request.order_details() != null ? request.order_details().total_amount() : null,
            request.product_details() != null ? request.product_details().size() : 0
        );

        return shadowfaxRestClient
            .post()
            .uri("/v3/clients/orders/")
            .body(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                log.error("Shadowfax marketplace order request failed with status={}", response.getStatusCode());

                throw new IllegalStateException("Shadowfax order creation failed: " + response.getStatusCode());
            })
            .body(ShadowfaxOrderResponse.class);
    }

    /*
     * Shadowfax V4 shipment tracking
     *
     * GET /v4/clients/orders/{awb_number}/track/
     */
    public ShadowfaxTrackingResponse trackOrder(String awbNumber) {
        if (awbNumber == null || awbNumber.isBlank()) {
            throw new IllegalArgumentException("AWB number is required");
        }

        log.info("Requesting Shadowfax tracking for AWB={}", awbNumber);

        return shadowfaxRestClient
            .get()
            .uri(uriBuilder -> uriBuilder.path("/v4/clients/orders/{awb}/track/").build(awbNumber))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                log.error("Shadowfax tracking request failed with status={}", response.getStatusCode());

                throw new IllegalStateException("Shadowfax tracking request failed: " + response.getStatusCode());
            })
            .body(ShadowfaxTrackingResponse.class);
    }

    public record ShadowfaxOrderRequest(
        String order_type,
        OrderDetails order_details,
        CustomerDetails customer_details,
        PickupDetails pickup_details,
        RtsDetails rts_details,
        List<ProductDetails> product_details
    ) {}

    public record OrderDetails(
        String client_order_id,
        String awb_number,
        BigDecimal actual_weight,
        BigDecimal volumetric_weight,
        BigDecimal product_value,
        BigDecimal cod_amount,
        String payment_mode,
        String promised_delivery_date,
        BigDecimal total_amount,
        String eway_bill,
        String gstin_number,
        String order_service
    ) {}

    public record CustomerDetails(
        String name,
        String contact,
        String alternate_contact,
        String address_line_1,
        String address_line_2,
        String city,
        String state,
        Integer pincode,
        String latitude,
        String longitude
    ) {}

    public record PickupDetails(
        String name,
        String contact,
        String address_line_1,
        String address_line_2,
        String city,
        String state,
        Integer pincode,
        String latitude,
        String longitude,
        String unique_code
    ) {}

    public record RtsDetails(
        String name,
        String contact,
        String address_line_1,
        String address_line_2,
        String city,
        String state,
        Integer pincode,
        String email,
        String latitude,
        String longitude,
        String unique_code
    ) {}

    public record ProductDetails(
        String hsn_code,
        String invoice_no,
        String sku_name,
        String sku_id,
        String category,
        BigDecimal price,
        SellerDetails seller_details,
        Taxes taxes,
        AdditionalDetails additional_details
    ) {}

    public record SellerDetails(String seller_name, String seller_address, String seller_state, String gstin_number) {}

    public record Taxes(BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total_tax) {}

    public record AdditionalDetails(String requires_extra_care, String type_extra_care, Integer quantity) {}

    public record ShadowfaxOrderResponse(String message, Object errors, ShadowfaxOrderData data) {}

    public record ShadowfaxOrderData(
        Long id,
        String client_name,
        String client_order_id,
        String awb_number,
        BigDecimal product_value,
        BigDecimal cod_amount,
        String payment_mode,
        String order_date,
        String promised_delivery_date,
        String status_display,
        String status
    ) {}

    /*
     * Shadowfax tracking response
     *
     * Structure based on the V4 tracking API supplied
     * in the Shadowfax API documentation.
     */
    public record ShadowfaxTrackingResponse(
        ShadowfaxTrackingOrderDetails order_details,
        List<ShadowfaxTrackingEvent> tracking_details
    ) {}

    public record ShadowfaxTrackingOrderDetails(
        Long id,
        String client_order_id,
        String awb_number,
        BigDecimal product_value,
        BigDecimal cod_amount,
        String payment_mode,
        String order_date,
        String promised_delivery_date,
        String status_display,
        String status,
        ShadowfaxPickupDetails pickup_details,
        ShadowfaxDeliveryDetails delivery_details,
        List<ShadowfaxTrackingProduct> product_details,
        String eway_bill_number,
        String invoice_date,
        String customer_track_url
    ) {}

    public record ShadowfaxTrackingEvent(
        String created,
        String location,
        String status_id,
        String status,
        String remarks,
        String awb_number
    ) {}

    public record ShadowfaxPickupDetails(
        String name,
        String contact,
        String address_line_1,
        String address_line_2,
        String city,
        String state,
        Integer pincode
    ) {}

    public record ShadowfaxDeliveryDetails(
        String name,
        String contact,
        String address_line_1,
        String address_line_2,
        String city,
        String state,
        Integer pincode
    ) {}

    public record ShadowfaxTrackingProduct(
        String hsn_code,
        String invoice_no,
        String sku_name,
        String sku_id,
        String category,
        BigDecimal price,
        Integer quantity
    ) {}

    public record AwbGenerationRequest(int count) {}

    public record AwbGenerationResponse(String message, List<String> awb_numbers) {}

    public record ServiceabilityResponse(Integer code, List<String> services) {}
}
