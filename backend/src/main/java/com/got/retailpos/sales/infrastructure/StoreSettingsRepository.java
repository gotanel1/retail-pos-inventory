package com.got.retailpos.sales.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import com.got.retailpos.sales.domain.StoreSettings;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Short> {}
