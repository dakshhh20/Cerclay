package com.mittiandmore.service;

import com.mittiandmore.dto.*;
import com.mittiandmore.entity.Discount;
import com.mittiandmore.exception.ApiException;
import com.mittiandmore.repository.DiscountRepository;
import com.mittiandmore.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscountService {

    private final DiscountRepository repo;
    private final OrderRepository orderRepository;

    public DiscountService(DiscountRepository repo, OrderRepository orderRepository) {
        this.repo = repo;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> all() {
        return repo.findAll().stream().map(DiscountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicDiscountResponse> available(Long customerId) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasPreviousOrder = customerId != null && orderRepository.existsByCustomerId(customerId);
        return repo
            .findAll()
            .stream()
            .filter(d -> isCurrentlyActive(d, now))
            .filter(d -> !Boolean.TRUE.equals(d.getFirstTimeOnly()) || !hasPreviousOrder)
            .map(PublicDiscountResponse::from)
            .toList();
    }

    @Transactional
    public DiscountResponse create(DiscountRequest r) {
        return DiscountResponse.from(save(new Discount(), r));
    }

    @Transactional
    public DiscountResponse update(Long id, DiscountRequest r) {
        Discount d = repo
            .findById(id)
            .orElseThrow(() -> new ApiException("DISCOUNT_NOT_FOUND", "Discount not found", HttpStatus.NOT_FOUND));
        return DiscountResponse.from(save(d, r));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ApiException(
            "DISCOUNT_NOT_FOUND",
            "Discount not found",
            HttpStatus.NOT_FOUND
        );
        repo.deleteById(id);
    }

    private Discount save(Discount d, DiscountRequest r) {
        String code = r.getCode().trim().toUpperCase();
        repo.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(d.getId())) throw new ApiException(
                "DISCOUNT_CODE_EXISTS",
                "Discount code already exists",
                HttpStatus.CONFLICT
            );
        });
        String type = r.getType().trim().toUpperCase();
        if (!type.equals("PERCENTAGE") && !type.equals("FIXED")) throw new ApiException(
            "INVALID_DISCOUNT_TYPE",
            "Type must be PERCENTAGE or FIXED",
            HttpStatus.BAD_REQUEST
        );
        if (type.equals("PERCENTAGE") && r.getValue().compareTo(BigDecimal.valueOf(100)) > 0) throw new ApiException(
            "INVALID_DISCOUNT_VALUE",
            "Percentage cannot exceed 100",
            HttpStatus.BAD_REQUEST
        );
        if (
            r.getExpiresAt() != null && r.getStartsAt() != null && r.getExpiresAt().isBefore(r.getStartsAt())
        ) throw new ApiException("INVALID_DISCOUNT_WINDOW", "Expiry must be after start", HttpStatus.BAD_REQUEST);
        d.setCode(code);
        d.setType(type);
        d.setValue(r.getValue());
        d.setMinimumOrderValue(r.getMinimumOrderValue());
        d.setMaxDiscount(r.getMaxDiscount());
        d.setStartsAt(r.getStartsAt());
        d.setExpiresAt(r.getExpiresAt());
        d.setUsageLimit(r.getUsageLimit());
        d.setActive(r.getActive() == null || r.getActive());
        d.setFirstTimeOnly(Boolean.TRUE.equals(r.getFirstTimeOnly()));
        return repo.save(d);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculate(String code, BigDecimal subtotal) {
        return calculate(code, subtotal, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculate(String code, BigDecimal subtotal, Long customerId) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        Discount d = repo
            .findByCodeIgnoreCase(code.trim())
            .orElseThrow(() -> new ApiException("INVALID_COUPON", "Invalid discount code", HttpStatus.BAD_REQUEST));
        validateAvailability(d, subtotal, LocalDateTime.now(), customerId);
        return calculateAmount(d, subtotal);
    }

    @Transactional
    public void releaseUsage(String code) {
        if (code == null || code.isBlank()) return;
        String normalized = code.trim().toUpperCase();
        repo.findByCodeIgnoreCaseForUpdate(normalized).ifPresent(d -> {
            if (d.getUsageCount() > 0) {
                d.setUsageCount(d.getUsageCount() - 1);
                repo.save(d);
            }
        });
    }

    @Transactional
    public BigDecimal calculateAndConsume(String code, BigDecimal subtotal) {
        return calculateAndConsume(code, subtotal, null);
    }

    @Transactional
    public BigDecimal calculateAndConsume(String code, BigDecimal subtotal, Long customerId) {
        if (code == null || code.isBlank()) return BigDecimal.ZERO;
        String normalized = code.trim().toUpperCase();
        Discount d = repo
            .findByCodeIgnoreCaseForUpdate(normalized)
            .orElseThrow(() -> new ApiException("INVALID_COUPON", "Invalid discount code", HttpStatus.BAD_REQUEST));
        validateAvailability(d, subtotal, LocalDateTime.now(), customerId);
        BigDecimal amount = calculateAmount(d, subtotal);
        d.setUsageCount(d.getUsageCount() + 1);
        repo.save(d);
        return amount;
    }

    private boolean isCurrentlyActive(Discount d, LocalDateTime now) {
        return (
            Boolean.TRUE.equals(d.getActive()) &&
            (d.getStartsAt() == null || !now.isBefore(d.getStartsAt())) &&
            (d.getExpiresAt() == null || !now.isAfter(d.getExpiresAt())) &&
            (d.getUsageLimit() == null || d.getUsageCount() < d.getUsageLimit())
        );
    }

    private void validateAvailability(Discount d, BigDecimal subtotal, LocalDateTime now, Long customerId) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) throw new ApiException(
            "INVALID_SUBTOTAL",
            "Subtotal is invalid",
            HttpStatus.BAD_REQUEST
        );
        if (!isCurrentlyActive(d, now)) throw new ApiException(
            "DISCOUNT_UNAVAILABLE",
            "Discount is not currently available",
            HttpStatus.BAD_REQUEST
        );
        if (Boolean.TRUE.equals(d.getFirstTimeOnly())) {
            if (customerId == null) throw new ApiException(
                "FIRST_TIME_OFFER_REQUIRES_LOGIN",
                "This offer is available to first-time customers after login",
                HttpStatus.UNAUTHORIZED
            );
            if (orderRepository.existsByCustomerId(customerId)) throw new ApiException(
                "FIRST_TIME_OFFER_UNAVAILABLE",
                "This offer is only available to first-time customers",
                HttpStatus.BAD_REQUEST
            );
        }
        if (subtotal.compareTo(d.getMinimumOrderValue()) < 0) throw new ApiException(
            "DISCOUNT_MINIMUM_NOT_MET",
            "Minimum order value for this discount is ₹" + d.getMinimumOrderValue(),
            HttpStatus.BAD_REQUEST
        );
    }

    private BigDecimal calculateAmount(Discount d, BigDecimal subtotal) {
        BigDecimal amount = d.getType().equals("PERCENTAGE")
            ? subtotal.multiply(d.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            : d.getValue();
        if (d.getMaxDiscount() != null) amount = amount.min(d.getMaxDiscount());
        return amount.min(subtotal).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
