package com.got.retailpos.inventory.infrastructure;

import java.util.UUID;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.got.retailpos.inventory.domain.StockMovement;
import com.got.retailpos.inventory.domain.MovementType;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

	Page<StockMovement> findAllByProductId(UUID productId, Pageable pageable);

	Page<StockMovement> findAllByMovementTypeIn(Collection<MovementType> types, Pageable pageable);
}
