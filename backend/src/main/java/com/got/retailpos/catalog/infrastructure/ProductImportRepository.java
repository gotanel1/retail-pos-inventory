package com.got.retailpos.catalog.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.got.retailpos.catalog.domain.ProductImport;

import jakarta.persistence.LockModeType;

public interface ProductImportRepository extends JpaRepository<ProductImport, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select productImport from ProductImport productImport where productImport.id = :id")
	Optional<ProductImport> findByIdForUpdate(@Param("id") UUID id);
}
