package com.got.retailpos.inventory.web;

import java.math.BigDecimal;
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
import com.got.retailpos.inventory.application.InventoryService;
import com.got.retailpos.inventory.domain.GoodsReceipt;
import com.got.retailpos.inventory.domain.GoodsReceiptItem;
import com.got.retailpos.inventory.domain.GoodsReceiptStatus;
import com.got.retailpos.shared.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/goods-receipts")
public class GoodsReceiptController {

	private final InventoryService service;

	public GoodsReceiptController(InventoryService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ReceiptSummaryResponse> findAll(
			@PageableDefault(size = 20, sort = "receivedAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
			Pageable pageable) {
		return PageResponse.from(service.findReceipts(pageable).map(ReceiptSummaryResponse::from));
	}

	@GetMapping("/{id}")
	public ReceiptResponse findById(@PathVariable UUID id) {
		return toResponse(service.findReceipt(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public ReceiptResponse create(
			@Valid @RequestBody ReceiptRequest body,
			Authentication authentication) {
		var receipt = service.postReceipt(body.toInput(), principal(authentication).id());
		return toResponse(receipt);
	}

	private ReceiptResponse toResponse(GoodsReceipt receipt) {
		var products = service.findProducts(receipt.getItems().stream().map(GoodsReceiptItem::getProductId).toList());
		return ReceiptResponse.from(receipt, products);
	}

	private RetailUserPrincipal principal(Authentication authentication) {
		return (RetailUserPrincipal) authentication.getPrincipal();
	}

	public record ReceiptRequest(
			@NotNull UUID supplierId,
			@Size(max = 100) String referenceNumber,
			@NotNull @PastOrPresent Instant receivedAt,
			@Size(max = 500) String note,
			@NotEmpty @Size(max = 200) List<@Valid ReceiptItemRequest> items) {

		InventoryService.ReceiptInput toInput() {
			return new InventoryService.ReceiptInput(
					supplierId,
					referenceNumber,
					receivedAt,
					note,
					items.stream().map(ReceiptItemRequest::toInput).toList());
		}
	}

	public record ReceiptItemRequest(
			@NotNull UUID productId,
			@Positive int quantity,
			@NotNull @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal unitCost) {

		InventoryService.ReceiptItemInput toInput() {
			return new InventoryService.ReceiptItemInput(productId, quantity, unitCost);
		}
	}

	public record ReceiptSummaryResponse(
			UUID id,
			UUID supplierId,
			String supplierName,
			String referenceNumber,
			GoodsReceiptStatus status,
			Instant receivedAt,
			UUID receivedBy) {

		static ReceiptSummaryResponse from(GoodsReceipt receipt) {
			return new ReceiptSummaryResponse(
					receipt.getId(),
					receipt.getSupplier().getId(),
					receipt.getSupplier().getName(),
					receipt.getReferenceNumber(),
					receipt.getStatus(),
					receipt.getReceivedAt(),
					receipt.getReceivedBy());
		}
	}

	public record ReceiptResponse(
			UUID id,
			UUID supplierId,
			String supplierName,
			String referenceNumber,
			GoodsReceiptStatus status,
			Instant receivedAt,
			String note,
			UUID receivedBy,
			List<ReceiptItemResponse> items,
			BigDecimal totalCost) {

		static ReceiptResponse from(GoodsReceipt receipt, java.util.Map<UUID, CatalogProduct> products) {
			var itemResponses = receipt.getItems().stream()
					.map(item -> ReceiptItemResponse.from(item, products.get(item.getProductId())))
					.toList();
			var total = itemResponses.stream()
					.map(ReceiptItemResponse::lineCost)
					.reduce(BigDecimal.ZERO.setScale(4), BigDecimal::add);
			return new ReceiptResponse(
					receipt.getId(),
					receipt.getSupplier().getId(),
					receipt.getSupplier().getName(),
					receipt.getReferenceNumber(),
					receipt.getStatus(),
					receipt.getReceivedAt(),
					receipt.getNote(),
					receipt.getReceivedBy(),
					itemResponses,
					total);
		}
	}

	public record ReceiptItemResponse(
			UUID productId,
			String sku,
			String productName,
			int quantity,
			BigDecimal unitCost,
			BigDecimal lineCost) {

		static ReceiptItemResponse from(GoodsReceiptItem item, CatalogProduct product) {
			return new ReceiptItemResponse(
					item.getProductId(),
					product == null ? "ไม่พบสินค้า" : product.sku(),
					product == null ? "สินค้าที่ไม่ใช้งาน" : product.name(),
					item.getQuantity(),
					item.getUnitCost(),
					item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())));
		}
	}
}
