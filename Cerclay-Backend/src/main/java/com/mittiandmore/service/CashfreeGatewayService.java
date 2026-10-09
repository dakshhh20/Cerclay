package com.mittiandmore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mittiandmore.config.CashfreeProperties;
import com.mittiandmore.exception.ApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CashfreeGatewayService {

    private final CashfreeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CashfreeGatewayService(CashfreeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode createOrder(
        String orderId,
        String customerId,
        String name,
        String email,
        String phone,
        java.math.BigDecimal amount,
        String returnUrl
    ) {
        requireConfigured();
        String json =
            "{" +
            "\"order_id\":\"" +
            esc(orderId) +
            "\"," +
            "\"order_amount\":" +
            amount.setScale(2).toPlainString() +
            "," +
            "\"order_currency\":\"INR\"," +
            "\"customer_details\":{" +
            "\"customer_id\":\"" +
            esc(customerId) +
            "\"," +
            "\"customer_name\":\"" +
            esc(name) +
            "\"," +
            "\"customer_email\":\"" +
            esc(email) +
            "\"," +
            "\"customer_phone\":\"" +
            esc(phone) +
            "\"}," +
            "\"order_meta\":{\"return_url\":\"" +
            esc(returnUrl) +
            "\"}" +
            "}";
        return send("POST", "/orders", json, UUID.randomUUID().toString());
    }

    public JsonNode getPayments(String orderId) {
        requireConfigured();
        return send("GET", "/orders/" + encode(orderId) + "/payments", null, null);
    }

    public JsonNode createRefund(String orderId, String refundId, java.math.BigDecimal amount, String note) {
        requireConfigured();
        String json =
            "{" +
            "\"refund_id\":\"" +
            esc(refundId) +
            "\"," +
            "\"refund_amount\":" +
            amount.setScale(2).toPlainString() +
            "," +
            "\"refund_note\":\"" +
            esc(note == null ? "Cerclay refund" : note) +
            "\"," +
            "\"refund_speed\":\"STANDARD\"" +
            "}";
        return send("POST", "/orders/" + encode(orderId) + "/refunds", json, refundId);
    }

    public JsonNode getRefund(String orderId, String refundId) {
        requireConfigured();
        return send("GET", "/orders/" + encode(orderId) + "/refunds/" + encode(refundId), null, null);
    }

    public void verifyWebhook(String timestamp, String signature, String rawBody) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new ApiException(
                "CASHFREE_WEBHOOK_NOT_CONFIGURED",
                "Cashfree webhook is not configured",
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
        if (timestamp == null || timestamp.isBlank() || signature == null || signature.isBlank()) {
            throw new ApiException(
                "INVALID_WEBHOOK_SIGNATURE",
                "Missing Cashfree webhook signature",
                HttpStatus.UNAUTHORIZED
            );
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + rawBody).getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(digest);
            if (
                !MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
                )
            ) {
                throw new ApiException(
                    "INVALID_WEBHOOK_SIGNATURE",
                    "Invalid Cashfree webhook signature",
                    HttpStatus.UNAUTHORIZED
                );
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(
                "INVALID_WEBHOOK_SIGNATURE",
                "Unable to verify Cashfree webhook signature",
                HttpStatus.UNAUTHORIZED
            );
        }
    }

    public CashfreeProperties getProperties() {
        return properties;
    }

    private JsonNode send(String method, String path, String body, String idempotencyKey) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl().replaceAll("/$", "") + path))
                .header("x-client-id", properties.getClientId())
                .header("x-client-secret", properties.getClientSecret())
                .header("x-api-version", properties.getApiVersion())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");
            if (idempotencyKey != null) builder.header("x-idempotency-key", idempotencyKey);
            if ("POST".equals(method)) builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            else builder.GET();
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node =
                response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = node.path("message").asText("Cashfree API request failed");
                throw new ApiException("CASHFREE_API_ERROR", message, HttpStatus.BAD_GATEWAY);
            }
            return node;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("CASHFREE_API_ERROR", "Unable to communicate with Cashfree", HttpStatus.BAD_GATEWAY);
        }
    }

    private void requireConfigured() {
        if (!properties.isEnabled() || blank(properties.getClientId()) || blank(properties.getClientSecret())) {
            throw new ApiException(
                "CASHFREE_NOT_CONFIGURED",
                "Cashfree is not configured",
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
