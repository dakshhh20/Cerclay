package com.mittiandmore.service;

import com.mittiandmore.entity.StoreSettings;
import com.mittiandmore.repository.StoreSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StoreSettingsService {

    private final StoreSettingsRepository storeSettingsRepository;

    public StoreSettingsService(
            StoreSettingsRepository storeSettingsRepository) {
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @Transactional(readOnly = true)
    public StoreSettings getSettings() {
        return storeSettingsRepository.findById(1L)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Store settings not found"
                        )
                );
    }

    @Transactional
    public StoreSettings updateSettings(
            BigDecimal gstRate,
            BigDecimal shippingCharge,
            BigDecimal freeShippingThreshold,
            BigDecimal minimumOrderValue,
            String whatsappNumber) {

        validateSettings(
                gstRate,
                shippingCharge,
                freeShippingThreshold,
                minimumOrderValue
        );

        String normalizedWhatsapp = whatsappNumber == null ? null : whatsappNumber.replaceAll("\\D", "");
        if (normalizedWhatsapp != null && normalizedWhatsapp.isBlank()) {
            normalizedWhatsapp = null;
        }
        if (normalizedWhatsapp != null && (normalizedWhatsapp.length() < 10 || normalizedWhatsapp.length() > 15)) {
            throw new IllegalArgumentException("WhatsApp number must contain 10 to 15 digits");
        }

        StoreSettings settings = getSettings();

        settings.setGstRate(gstRate);
        settings.setShippingCharge(shippingCharge);
        settings.setFreeShippingThreshold(freeShippingThreshold);
        settings.setMinimumOrderValue(minimumOrderValue);
        settings.setWhatsappNumber(normalizedWhatsapp);

        return storeSettingsRepository.save(settings);
    }

    private void validateSettings(
            BigDecimal gstRate,
            BigDecimal shippingCharge,
            BigDecimal freeShippingThreshold,
            BigDecimal minimumOrderValue) {

        if (gstRate == null || gstRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "GST rate cannot be negative"
            );
        }

        if (shippingCharge == null
                || shippingCharge.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Shipping charge cannot be negative"
            );
        }

        if (freeShippingThreshold == null
                || freeShippingThreshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Free shipping threshold cannot be negative"
            );
        }

        if (minimumOrderValue == null
                || minimumOrderValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value cannot be negative"
            );
        }
    }
}