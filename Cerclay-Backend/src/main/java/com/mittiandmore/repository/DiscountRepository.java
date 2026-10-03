package com.mittiandmore.repository;

import com.mittiandmore.entity.Discount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Discount d WHERE LOWER(d.code) = LOWER(:code)")
    Optional<Discount> findByCodeIgnoreCaseForUpdate(@Param("code") String code);
}
