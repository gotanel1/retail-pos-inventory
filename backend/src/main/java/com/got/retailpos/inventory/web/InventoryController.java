package com.got.retailpos.inventory.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.inventory.application.InventoryService;
import com.got.retailpos.inventory.domain.MovementType;
import com.got.retailpos.inventory.domain.StockMovement;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository.BalanceView;
import com.got.retailpos.shared.web.PageResponse;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

	private final InventoryService service;

	public InventoryController(InventoryService service) {
		this.service = service;
	}

	@GetMapping("/balances")
	public PageResponse<BalanceResponse> findBalances(
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "false") boolean lowStock,
			@PageableDefault(size = 20, sort = "sku") Pageable pageable) {
		return PageResponse.from(service.findBalances(search, lowStock, pageable).map(BalanceResponse::from));
	}

	@GetMapping("/movements")
	public PageResponse<MovementResponse> findMovements(
			@RequestParam(required = false) UUID productId,
			@PageableDefault(size = 20, sort = "occurredAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
			Pageable pageable) {
		var page = service.findMovements(productId, pageable);
		var productIds = page.stream().map(StockMovement::getProductId).distinct().toList();
		var products = service.findProducts(productIds);
		return PageResponse.from(page.map(movement -> MovementResponse.from(
				movement,
				products.get(movement.getProductId()))));
	}

	public record BalanceResponse(
			UUID productId,
			String sku,
			String productName,
			int lowStockThreshold,
			int onHand,
			int reserved,
			int available,
			BigDecimal averageCost,
			boolean lowStock) {

		static BalanceResponse from(BalanceView view) {
			return new BalanceResponse(
					view.getProductId(),
					view.getSku(),
					view.getProductName(),
					view.getLowStockThreshold(),
					view.getOnHand(),
					view.getReserved(),
					view.getAvailable(),
					view.getAverageCost(),
					view.getAvailable() <= view.getLowStockThreshold());
		}
	}

	public record MovementResponse(
			UUID id,
			UUID productId,
			String sku,
			String productName,
			MovementType movementType,
			int quantityDelta,
			int onHandAfter,
			int reservedAfter,
			BigDecimal unitCost,
			BigDecimal averageCostAfter,
			String referenceType,
			UUID referenceId,
			String reason,
			UUID actorUserId,
			Instant occurredAt) {

		static MovementResponse from(
				StockMovement movement,
				com.got.retailpos.catalog.application.ProductCatalogReader.CatalogProduct product) {
			return new MovementResponse(
					movement.getId(),
					movement.getProductId(),
					product == null ? "ไม่พบสินค้า" : product.sku(),
					product == null ? "สินค้าที่ไม่ใช้งาน" : product.name(),
					movement.getMovementType(),
					movement.getQuantityDelta(),
					movement.getOnHandAfter(),
					movement.getReservedAfter(),
					movement.getUnitCost(),
					movement.getAverageCostAfter(),
					movement.getReferenceType(),
					movement.getReferenceId(),
					movement.getReason(),
					movement.getActorUserId(),
					movement.getOccurredAt());
		}
	}
}
