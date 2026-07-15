package com.got.retailpos.inventory.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.got.retailpos.inventory.domain.InventoryCount;

import jakarta.persistence.LockModeType;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, UUID> {

	@EntityGraph(attributePaths = "items")
	Optional<InventoryCount> findOneById(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "items")
	@Query("select count from InventoryCount count where count.id = :id")
	Optional<InventoryCount> findByIdForUpdate(@Param("id") UUID id);

	Page<InventoryCount> findAllBy(Pageable pageable);
}
