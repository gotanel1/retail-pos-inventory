package com.got.retailpos.inventory.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.got.retailpos.inventory.domain.GoodsReceipt;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

	boolean existsByReferenceNumber(String referenceNumber);

	@EntityGraph(attributePaths = { "supplier", "items" })
	Optional<GoodsReceipt> findOneById(UUID id);

	@EntityGraph(attributePaths = "supplier")
	Page<GoodsReceipt> findAllBy(Pageable pageable);
}
