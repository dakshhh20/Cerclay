package com.mittiandmore.repository;

import com.mittiandmore.entity.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository
        extends JpaRepository<StoreSettings, Long> {
}