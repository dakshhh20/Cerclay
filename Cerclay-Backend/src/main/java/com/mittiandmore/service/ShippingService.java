package com.mittiandmore.service;

import com.mittiandmore.dto.ShippingPincodeRequest;
import com.mittiandmore.dto.ShippingPincodeResponse;
import com.mittiandmore.dto.ShippingQuoteRequest;
import com.mittiandmore.dto.ShippingQuoteResponse;
import com.mittiandmore.dto.ShippingZoneRequest;
import com.mittiandmore.dto.ShippingZoneResponse;
import com.mittiandmore.entity.ShippingPincode;
import com.mittiandmore.entity.ShippingZone;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.ShippingPincodeRepository;
import com.mittiandmore.repository.ShippingZoneRepository;
import com.mittiandmore.config.ShadowfaxProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class ShippingService {

    /*
     * Mitti & More customer shipping is distance/zone based.
     * The shipping charge is determined from the destination pincode
     * mapping to Zone A/B/C/D/E, with optional business rules such as
     * free-delivery threshold and COD surcharge. Product weight and
     * volumetric weight are intentionally not part of this engine.
     */

    private final ShippingZoneRepository zoneRepository;
    private final ShippingPincodeRepository pincodeRepository;
    private final ShadowfaxClient shadowfaxClient;
    private final ShadowfaxProperties shadowfaxProperties;
    private final PincodeZoneClassifier pincodeZoneClassifier;
    private final StoreSettingsService storeSettingsService;

    public ShippingService(
            ShippingZoneRepository zoneRepository,
            ShippingPincodeRepository pincodeRepository,
            ShadowfaxClient shadowfaxClient,
            ShadowfaxProperties shadowfaxProperties,
            PincodeZoneClassifier pincodeZoneClassifier,
            StoreSettingsService storeSettingsService) {
        this.zoneRepository = zoneRepository;
        this.pincodeRepository = pincodeRepository;
        this.shadowfaxClient = shadowfaxClient;
        this.shadowfaxProperties = shadowfaxProperties;
        this.pincodeZoneClassifier = pincodeZoneClassifier;
        this.storeSettingsService = storeSettingsService;
    }

    @Transactional(readOnly = true)
    public ShippingQuoteResponse calculateQuote(ShippingQuoteRequest request) {
        String pincode = normalizePincode(request.getPincode());
        BigDecimal orderValue = money(request.getOrderValue());
        if (orderValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(
                    "INVALID_ORDER_VALUE",
                    "Order value cannot be negative.",
                    HttpStatus.BAD_REQUEST
            );
        }
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

        boolean serviceable = isServiceableByShadowfax(pincode);
        if (!serviceable) {
            ShippingQuoteResponse response = new ShippingQuoteResponse();
            response.setPincode(pincode);
            response.setServiceable(false);
            response.setMessage("Sorry, we currently do not deliver to " + pincode + ".");
            return response;
        }

        ShippingPincode mapping = pincodeRepository.findByPincode(pincode).orElse(null);

        ShippingZone zone;
        if (mapping != null) {
            // An explicit inactive pincode is an intentional admin block.
            // Do not silently fall back to automatic classification.
            if (!Boolean.TRUE.equals(mapping.getActive())) {
                ShippingQuoteResponse response = new ShippingQuoteResponse();
                response.setPincode(pincode);
                response.setServiceable(false);
                response.setMessage("Sorry, delivery is currently unavailable to " + pincode + ".");
                return response;
            }
            // Explicit admin mapping always wins over automatic classification.
            zone = mapping.getZone();
        } else {
            String pickupPincode = shadowfaxProperties.getPickupPincode();
            String pickupState = shadowfaxProperties.getPickupState();
            if (pickupPincode == null || pickupPincode.isBlank() || pickupState == null || pickupState.isBlank()) {
                throw new ApiException(
                        "SHIPPING_ZONE_NOT_CONFIGURED",
                        "Automatic shipping-zone classification requires a configured pickup pincode and pickup state.",
                        HttpStatus.CONFLICT
                );
            }

            PincodeZoneClassifier.Zone zoneCode = pincodeZoneClassifier.classify(pincode, pickupPincode, pickupState);
            zone = zoneRepository.findByCode(zoneCode.name())
                    .orElseThrow(() -> new ApiException(
                            "SHIPPING_ZONE_NOT_CONFIGURED",
                            "Automatic shipping zone " + zoneCode.name() + " is not configured.",
                            HttpStatus.CONFLICT
                    ));
        }
        if (!Boolean.TRUE.equals(zone.getActive())) {
            throw new ApiException(
                    "SHIPPING_ZONE_INACTIVE",
                    "Shipping is temporarily unavailable for this pincode.",
                    HttpStatus.CONFLICT
            );
        }

        BigDecimal shadowfaxBaseRate = zone.getShadowfaxBaseRate();
        BigDecimal customerCharge = mapping == null
                ? zone.getCustomerCharge()
                : firstNonNull(mapping.getCustomerChargeOverride(), zone.getCustomerCharge());

        // The admin store setting is the global free-delivery threshold.
        // Explicit pincode/zone thresholds remain supported as more specific
        // shipping rules; when they are not configured, the global threshold
        // is used for the checkout quote.
        BigDecimal globalFreeThreshold = storeSettingsService.getSettings().getFreeShippingThreshold();
        BigDecimal zoneFreeThreshold = zone.getFreeDeliveryThreshold();
        BigDecimal freeThreshold;
        if (mapping != null && mapping.getFreeDeliveryThresholdOverride() != null) {
            freeThreshold = mapping.getFreeDeliveryThresholdOverride();
        } else if (zoneFreeThreshold != null
                && zoneFreeThreshold.compareTo(BigDecimal.ZERO) > 0) {
            freeThreshold = zoneFreeThreshold;
        } else {
            freeThreshold = globalFreeThreshold;
        }
        if (freeThreshold == null) {
            freeThreshold = BigDecimal.ZERO;
        }

        BigDecimal codCharge = "COD".equals(paymentMethod)
                ? (mapping == null
                    ? zone.getCodCharge()
                    : firstNonNull(mapping.getCodChargeOverride(), zone.getCodCharge()))
                : BigDecimal.ZERO;

        boolean freeDelivery = freeThreshold.compareTo(BigDecimal.ZERO) > 0
                && orderValue.compareTo(freeThreshold) >= 0;

        BigDecimal finalCharge = freeDelivery
                ? BigDecimal.ZERO
                : customerCharge.add(codCharge);

        ShippingQuoteResponse response = new ShippingQuoteResponse();
        response.setPincode(pincode);
        response.setServiceable(true);
        response.setService(findServiceName(pincode));
        response.setZoneCode(zone.getCode());
        response.setZoneName(zone.getName());
        response.setShadowfaxBaseRate(money(shadowfaxBaseRate));
        response.setCustomerShippingCharge(money(finalCharge));
        response.setCodCharge(money(codCharge));
        response.setFreeDeliveryThreshold(money(freeThreshold));
        response.setFreeDeliveryApplied(freeDelivery);
        response.setMessage(freeDelivery
                ? "Free delivery available for this order."
                : "Delivery available to " + pincode + ".");
        return response;
    }

    @Transactional(readOnly = true)
    public boolean isServiceableByShadowfax(String pincode) {
        String normalized = normalizePincode(pincode);
        try {
            return shadowfaxClient.checkCustomerDeliveryServiceability(normalized)
                    .stream()
                    .anyMatch(result -> result.code() != null
                            && normalized.equals(String.valueOf(result.code()))
                            && result.services() != null
                            && !result.services().isEmpty());
        } catch (RuntimeException exception) {
            throw new ApiException(
                    "SHIPPING_SERVICEABILITY_UNAVAILABLE",
                    "We could not verify delivery serviceability right now. Please try again.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ShippingZoneResponse> getZones() {
        return zoneRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toZoneResponse)
                .toList();
    }

    @Transactional
    public ShippingZoneResponse createZone(ShippingZoneRequest request) {
        String code = normalizeCode(request.getCode());
        if (zoneRepository.findByCode(code).isPresent()) {
            throw new ApiException("DUPLICATE_SHIPPING_ZONE", "Shipping zone code already exists", HttpStatus.CONFLICT);
        }
        ShippingZone zone = new ShippingZone();
        applyZone(zone, request, code);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public ShippingZoneResponse updateZone(Long id, ShippingZoneRequest request) {
        ShippingZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ApiException("SHIPPING_ZONE_NOT_FOUND", "Shipping zone not found", HttpStatus.NOT_FOUND));
        String code = normalizeCode(request.getCode());
        zoneRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException("DUPLICATE_SHIPPING_ZONE", "Shipping zone code already exists", HttpStatus.CONFLICT);
                });
        applyZone(zone, request, code);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ShippingPincodeResponse> getPincodes() {
        return pincodeRepository.findAllByOrderByPincodeAsc()
                .stream()
                .map(this::toPincodeResponse)
                .toList();
    }

    @Transactional
    public ShippingPincodeResponse createPincode(ShippingPincodeRequest request) {
        String pincode = normalizePincode(request.getPincode());
        if (pincodeRepository.existsByPincode(pincode)) {
            throw new ApiException("DUPLICATE_SHIPPING_PINCODE", "Shipping pincode already exists", HttpStatus.CONFLICT);
        }
        ShippingPincode mapping = new ShippingPincode();
        applyPincode(mapping, request, pincode);
        return toPincodeResponse(pincodeRepository.save(mapping));
    }

    @Transactional
    public ShippingPincodeResponse updatePincode(Long id, ShippingPincodeRequest request) {
        ShippingPincode mapping = pincodeRepository.findById(id)
                .orElseThrow(() -> new ApiException("SHIPPING_PINCODE_NOT_FOUND", "Shipping pincode not found", HttpStatus.NOT_FOUND));
        String pincode = normalizePincode(request.getPincode());
        pincodeRepository.findByPincode(pincode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException("DUPLICATE_SHIPPING_PINCODE", "Shipping pincode already exists", HttpStatus.CONFLICT);
                });
        applyPincode(mapping, request, pincode);
        return toPincodeResponse(pincodeRepository.save(mapping));
    }

    @Transactional
    public void deletePincode(Long id) {
        ShippingPincode mapping = pincodeRepository.findById(id)
                .orElseThrow(() -> new ApiException("SHIPPING_PINCODE_NOT_FOUND", "Shipping pincode not found", HttpStatus.NOT_FOUND));
        pincodeRepository.delete(mapping);
    }

    private void applyZone(ShippingZone zone, ShippingZoneRequest request, String code) {
        zone.setCode(code);
        zone.setName(request.getName().trim());
        zone.setShadowfaxBaseRate(money(request.getShadowfaxBaseRate()));
        zone.setCustomerCharge(money(request.getCustomerCharge()));
        zone.setFreeDeliveryThreshold(money(request.getFreeDeliveryThreshold()));
        zone.setCodCharge(money(request.getCodCharge()));
        zone.setActive(request.getActive() == null || request.getActive());
    }

    private void applyPincode(ShippingPincode mapping, ShippingPincodeRequest request, String pincode) {
        ShippingZone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ApiException("SHIPPING_ZONE_NOT_FOUND", "Shipping zone not found", HttpStatus.NOT_FOUND));
        mapping.setPincode(pincode);
        mapping.setZone(zone);
        mapping.setCustomerChargeOverride(nonNegativeOrNull(request.getCustomerChargeOverride()));
        mapping.setFreeDeliveryThresholdOverride(nonNegativeOrNull(request.getFreeDeliveryThresholdOverride()));
        mapping.setCodChargeOverride(nonNegativeOrNull(request.getCodChargeOverride()));
        mapping.setActive(request.getActive() == null || request.getActive());
    }

    private ShippingZoneResponse toZoneResponse(ShippingZone zone) {
        ShippingZoneResponse response = new ShippingZoneResponse();
        response.setId(zone.getId());
        response.setCode(zone.getCode());
        response.setName(zone.getName());
        response.setShadowfaxBaseRate(money(zone.getShadowfaxBaseRate()));
        response.setCustomerCharge(money(zone.getCustomerCharge()));
        response.setFreeDeliveryThreshold(money(zone.getFreeDeliveryThreshold()));
        response.setCodCharge(money(zone.getCodCharge()));
        response.setActive(zone.getActive());
        response.setUpdatedAt(zone.getUpdatedAt());
        return response;
    }

    private ShippingPincodeResponse toPincodeResponse(ShippingPincode mapping) {
        ShippingPincodeResponse response = new ShippingPincodeResponse();
        response.setId(mapping.getId());
        response.setPincode(mapping.getPincode());
        response.setZoneId(mapping.getZone().getId());
        response.setZoneCode(mapping.getZone().getCode());
        response.setZoneName(mapping.getZone().getName());
        response.setCustomerChargeOverride(mapping.getCustomerChargeOverride());
        response.setFreeDeliveryThresholdOverride(mapping.getFreeDeliveryThresholdOverride());
        response.setCodChargeOverride(mapping.getCodChargeOverride());
        response.setActive(mapping.getActive());
        response.setUpdatedAt(mapping.getUpdatedAt());
        return response;
    }

    private String findServiceName(String pincode) {
        return shadowfaxClient.checkCustomerDeliveryServiceability(pincode)
                .stream()
                .filter(result -> result.code() != null && pincode.equals(String.valueOf(result.code())))
                .flatMap(result -> result.services() == null ? java.util.stream.Stream.empty() : result.services().stream())
                .findFirst()
                .orElse("Regular");
    }

    private String normalizePincode(String pincode) {
        if (pincode == null || !pincode.trim().matches("\\d{6}")) {
            throw new ApiException("INVALID_PINCODE", "Pincode must contain exactly 6 digits", HttpStatus.BAD_REQUEST);
        }
        return pincode.trim();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "CASHFREE" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"COD".equals(normalized) && !"CASHFREE".equals(normalized) && !"RAZORPAY".equals(normalized)) {
            throw new ApiException("INVALID_PAYMENT_METHOD", "Payment method must be COD, CASHFREE or RAZORPAY", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException("INVALID_SHIPPING_ZONE", "Shipping zone code is required", HttpStatus.BAD_REQUEST);
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal firstNonNull(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private BigDecimal nonNegativeOrNull(BigDecimal value) {
        if (value == null) return null;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("INVALID_SHIPPING_RULE", "Shipping override cannot be negative", HttpStatus.BAD_REQUEST);
        }
        return money(value);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
