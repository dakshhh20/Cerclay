package com.mittiandmore.repository;

import com.mittiandmore.entity.CustomerOtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOtpVerificationRepository
        extends JpaRepository<CustomerOtpVerification, Long> {

    Optional<CustomerOtpVerification>
    findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String destination,
            String purpose
    );

    List<CustomerOtpVerification>
    findByDestinationAndPurposeAndVerifiedAtIsNull(
            String destination,
            String purpose
    );
}