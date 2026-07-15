package com.got.retailpos.inventory.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.got.retailpos.inventory.domain.InventoryBalance;

import jakarta.persistence.LockModeType;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {

	@Modifying
	@Query(value = """
			INSERT INTO inventory_balances (product_id, on_hand, reserved, average_cost, version, updated_at)
			VALUES (:productId, 0, 0, 0, 0, CURRENT_TIMESTAMP)
			ON CONFLICT (product_id) DO NOTHING
			""", nativeQuery = true)
	void ensureExists(@Param("productId") UUID productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select balance from InventoryBalance balance where balance.productId = :productId")
	Optional<InventoryBalance> findByProductIdForUpdate(@Param("productId") UUID productId);

	@Query(value = """
			SELECT p.id AS productId,
			       p.sku AS sku,
			       p.name AS productName,
			       p.low_stock_threshold AS lowStockThreshold,
			       COALESCE(b.on_hand, 0) AS onHand,
			       COALESCE(b.reserved, 0) AS reserved,
			       COALESCE(b.on_hand - b.reserved, 0) AS available,
			       COALESCE(b.average_cost, 0) AS averageCost
			FROM products p
			LEFT JOIN inventory_balances b ON b.product_id = p.id
			WHERE p.active = TRUE
			  AND (:search = '' OR lower(p.name) LIKE lower(concat('%', :search, '%'))
			       OR lower(p.sku) LIKE lower(concat('%', :search, '%'))
			       OR lower(COALESCE(p.barcode, '')) LIKE lower(concat('%', :search, '%')))
			  AND (:lowStock = FALSE OR COALESCE(b.on_hand - b.reserved, 0) <= p.low_stock_threshold)
			""",
			countQuery = """
			SELECT count(*)
			FROM products p
			LEFT JOIN inventory_balances b ON b.product_id = p.id
			WHERE p.active = TRUE
			  AND (:search = '' OR lower(p.name) LIKE lower(concat('%', :search, '%'))
			       OR lower(p.sku) LIKE lower(concat('%', :search, '%'))
			       OR lower(COALESCE(p.barcode, '')) LIKE lower(concat('%', :search, '%')))
			  AND (:lowStock = FALSE OR COALESCE(b.on_hand - b.reserved, 0) <= p.low_stock_threshold)
			""",
			nativeQuery = true)
	Page<BalanceView> findBalanceViews(
			@Param("search") String search,
			@Param("lowStock") boolean lowStock,
			Pageable pageable);

	interface BalanceView {
		UUID getProductId();
		String getSku();
		String getProductName();
		int getLowStockThreshold();
		int getOnHand();
		int getReserved();
		int getAvailable();
		java.math.BigDecimal getAverageCost();
	}
}
