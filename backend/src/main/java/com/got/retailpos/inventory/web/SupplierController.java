package com.got.retailpos.inventory.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.inventory.application.SupplierService;
import com.got.retailpos.inventory.domain.Supplier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

	private final SupplierService service;

	public SupplierController(SupplierService service) {
		this.service = service;
	}

	@GetMapping
	public List<SupplierResponse> findAll() {
		return service.findAll().stream().map(SupplierResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public SupplierResponse create(@Valid @RequestBody SupplierRequest body) {
		return SupplierResponse.from(service.create(body.toInput()));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'INVENTORY_STAFF')")
	public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest body) {
		return SupplierResponse.from(service.update(id, body.toInput()));
	}

	public record SupplierRequest(
			@NotBlank @Size(max = 180) String name,
			@Size(max = 32) String phone,
			@Size(max = 500) String note) {

		SupplierService.SupplierInput toInput() {
			return new SupplierService.SupplierInput(name, phone, note);
		}
	}

	public record SupplierResponse(UUID id, String name, String phone, String note, boolean active) {

		static SupplierResponse from(Supplier supplier) {
			return new SupplierResponse(
					supplier.getId(),
					supplier.getName(),
					supplier.getPhone(),
					supplier.getNote(),
					supplier.isActive());
		}
	}
}
