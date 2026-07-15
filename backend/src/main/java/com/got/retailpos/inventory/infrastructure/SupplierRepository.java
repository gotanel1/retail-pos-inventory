package com.got.retailpos.inventory.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.got.retailpos.inventory.domain.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

	boolean existsByNormalizedName(String normalizedName);

	boolean existsByNormalizedNameAndIdNot(String normalizedName, UUID id);

	List<Supplier> findAllByOrderByNameAsc();
}
