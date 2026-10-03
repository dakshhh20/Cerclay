package com.mittiandmore.repository;

import com.mittiandmore.entity.CustomerPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPasswordResetTokenRepository
        extends JpaRepository<CustomerPasswordResetToken, Long> {

    Optional<CustomerPasswordResetToken>
    findByTokenHash(String tokenHash);
}