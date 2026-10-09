package com.mittiandmore.repository;

import com.mittiandmore.entity.CustomerOtpVerification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOtpVerificationRepository extends JpaRepository<CustomerOtpVerification, Long> {
    Optional<CustomerOtpVerification> findTopByDestinationAndPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(
        String destination,
        String purpose
    );

    List<CustomerOtpVerification> findByDestinationAndPurposeAndVerifiedAtIsNull(String destination, String purpose);
}
