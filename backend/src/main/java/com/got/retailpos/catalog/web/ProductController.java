package com.got.retailpos.catalog.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.domain.Product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductCatalogService service;

	public ProductController(ProductCatalogService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductResponse> findAll(
			@RequestParam(required = false) String search,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return PageResponse.from(service.findProducts(search, pageable).map(ProductResponse::from));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public ProductResponse create(@Valid @RequestBody ProductRequest body) {
		return ProductResponse.from(service.createProduct(body.toInput()));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest body) {
		return ProductResponse.from(service.updateProduct(id, body.toInput()));
	}

	public record ProductRequest(
			@NotNull UUID categoryId,
			@NotBlank @Size(max = 80) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String sku,
			@Size(max = 64) @Pattern(regexp = "^[a-zA-Z0-9._-]*$") String barcode,
			@NotBlank @Size(max = 180) String name,
			@NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal salePrice,
			@Min(0) int lowStockThreshold) {

		ProductCatalogService.ProductInput toInput() {
			return new ProductCatalogService.ProductInput(
					categoryId, sku, barcode, name, salePrice, lowStockThreshold);
		}
	}

	public record ProductResponse(
			UUID id,
			UUID categoryId,
			String categoryName,
			String sku,
			String barcode,
			String name,
			BigDecimal salePrice,
			int lowStockThreshold,
			boolean active) {

		static ProductResponse from(Product product) {
			return new ProductResponse(
					product.getId(),
					product.getCategory().getId(),
					product.getCategory().getName(),
					product.getSku(),
					product.getBarcode(),
					product.getName(),
					product.getSalePrice(),
					product.getLowStockThreshold(),
					product.isActive());
		}
	}

	public record PageResponse<T>(
			List<T> content,
			int page,
			int size,
			long totalElements,
			int totalPages,
			boolean first,
			boolean last) {

		static <T> PageResponse<T> from(Page<T> result) {
			return new PageResponse<>(
					result.getContent(),
					result.getNumber(),
					result.getSize(),
					result.getTotalElements(),
					result.getTotalPages(),
					result.isFirst(),
					result.isLast());
		}
	}
}
