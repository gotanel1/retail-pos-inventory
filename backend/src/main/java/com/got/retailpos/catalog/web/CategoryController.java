package com.got.retailpos.catalog.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.catalog.application.ProductCatalogService;
import com.got.retailpos.catalog.domain.Category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final ProductCatalogService service;

	public CategoryController(ProductCatalogService service) {
		this.service = service;
	}

	@GetMapping
	public List<CategoryResponse> findAll() {
		return service.findCategories().stream().map(CategoryResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest body) {
		return CategoryResponse.from(service.createCategory(body.name()));
	}

	public record CreateCategoryRequest(@NotBlank @Size(max = 120) String name) {
	}

	public record CategoryResponse(UUID id, String name, boolean active) {

		static CategoryResponse from(Category category) {
			return new CategoryResponse(category.getId(), category.getName(), category.isActive());
		}
	}
}
