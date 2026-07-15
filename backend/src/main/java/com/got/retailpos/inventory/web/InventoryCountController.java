package com.got.retailpos.inventory.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.catalog.application.ProductCatalogReader.CatalogProduct;
import com.got.retailpos.identity.security.RetailUserPrincipal;
import com.got.retailpos.inventory.application.InventoryCountService;
import com.got.retailpos.inventory.domain.InventoryCount;
import com.got.retailpos.inventory.domain.InventoryCountItem;
import com.got.retailpos.inventory.domain.InventoryCountStatus;
import com.got.retailpos.inventory.domain.MovementType;
import com.got.retailpos.inventory.domain.StockMovement;
import com.got.retailpos.shared.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryCountController {

	private final InventoryCountService service;

	public InventoryCountController(InventoryCountService service) { this.service = service; }

	@GetMapping("/counts")
	public PageResponse<CountResponse> findAll(@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return PageResponse.from(service.findAll(pageable).map(this::toResponse));
	}

	@GetMapping("/counts/{id}")
	public CountResponse findById(@PathVariable UUID id) { return toResponse(service.findById(id)); }

	@PostMapping("/counts")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public CountResponse submit(@Valid @RequestBody CountRequest body, Authentication authentication) {
		return toResponse(service.submit(body.toInput(), principal(authentication).id()));
	}

	@PostMapping("/counts/{id}/approve")
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
	public CountResponse approve(@PathVariable UUID id, Authentication authentication) {
		return toResponse(service.approve(id, principal(authentication).id()));
	}

	@PostMapping("/counts/{id}/reject")
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
	public CountResponse reject(@PathVariable UUID id, Authentication authentication) {
		return toResponse(service.reject(id, principal(authentication).id()));
	}

	@GetMapping("/adjustments")
	public PageResponse<AdjustmentResponse> findAdjustments(@PageableDefault(size = 20, sort = "occurredAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return PageResponse.from(service.findAdjustments(pageable).map(AdjustmentResponse::from));
	}

	private CountResponse toResponse(InventoryCount count) {
		var products = service.findProducts(count.getItems().stream().map(InventoryCountItem::getProductId).toList());
		return CountResponse.from(count, products);
	}

	private RetailUserPrincipal principal(Authentication authentication) { return (RetailUserPrincipal) authentication.getPrincipal(); }

	public record CountRequest(@NotNull @PastOrPresent Instant countedAt, @NotNull @Size(min = 1, max = 500) String reason,
			@NotEmpty @Size(max = 200) List<@Valid CountItemRequest> items) {
		InventoryCountService.CountInput toInput() { return new InventoryCountService.CountInput(reason, countedAt, items.stream().map(CountItemRequest::toInput).toList()); }
	}

	public record CountItemRequest(@NotNull UUID productId, @PositiveOrZero int countedQuantity) {
		InventoryCountService.CountItemInput toInput() { return new InventoryCountService.CountItemInput(productId, countedQuantity); }
	}

	public record CountResponse(UUID id, InventoryCountStatus status, String reason, Instant countedAt, UUID createdBy,
			Instant createdAt, UUID approvedBy, Instant approvedAt, List<CountItemResponse> items) {
		static CountResponse from(InventoryCount count, java.util.Map<UUID, CatalogProduct> products) {
			return new CountResponse(count.getId(), count.getStatus(), count.getReason(), count.getCountedAt(), count.getCreatedBy(),
					count.getCreatedAt(), count.getApprovedBy(), count.getApprovedAt(), count.getItems().stream().map(item -> CountItemResponse.from(item, products.get(item.getProductId()))).toList());
		}
	}

	public record CountItemResponse(UUID productId, String sku, String productName, int expectedOnHand, int countedQuantity, int difference) {
		static CountItemResponse from(InventoryCountItem item, CatalogProduct product) {
			return new CountItemResponse(item.getProductId(), product == null ? "ไม่พบสินค้า" : product.sku(), product == null ? "สินค้าที่ไม่ใช้งาน" : product.name(),
					item.getExpectedOnHand(), item.getCountedQuantity(), item.getDifference());
		}
	}

	public record AdjustmentResponse(UUID id, UUID productId, MovementType movementType, int quantityDelta, int onHandAfter,
			UUID countId, String reason, UUID approvedBy, Instant occurredAt) {
		static AdjustmentResponse from(StockMovement movement) {
			return new AdjustmentResponse(movement.getId(), movement.getProductId(), movement.getMovementType(), movement.getQuantityDelta(),
					movement.getOnHandAfter(), movement.getReferenceId(), movement.getReason(), movement.getActorUserId(), movement.getOccurredAt());
		}
	}
}
