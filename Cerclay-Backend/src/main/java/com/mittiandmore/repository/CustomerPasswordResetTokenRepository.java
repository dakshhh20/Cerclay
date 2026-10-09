package com.mittiandmore.repository;

import com.mittiandmore.entity.CustomerPasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerPasswordResetTokenRepository extends JpaRepository<CustomerPasswordResetToken, Long> {
    Optional<CustomerPasswordResetToken> findByTokenHash(String tokenHash);
}
