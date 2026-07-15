package com.got.retailpos.customers.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.got.retailpos.customers.application.CustomerService;
import com.got.retailpos.customers.domain.Customer;
import com.got.retailpos.shared.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
public class CustomerController {

	private final CustomerService service;

	public CustomerController(CustomerService service) { this.service = service; }

	@GetMapping
	public PageResponse<CustomerResponse> findAll(@RequestParam(required = false) String search,
			@PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return PageResponse.from(service.findAll(search, pageable).map(CustomerResponse::from));
	}

	@GetMapping("/{id}")
	public CustomerResponse findById(@PathVariable UUID id) { return CustomerResponse.from(service.findById(id)); }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CustomerResponse create(@Valid @RequestBody CustomerRequest body) { return CustomerResponse.from(service.create(body.toInput())); }

	@PutMapping("/{id}")
	public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest body) { return CustomerResponse.from(service.update(id, body.toInput())); }

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
	public void anonymize(@PathVariable UUID id) { service.anonymize(id); }

	public record CustomerRequest(@NotBlank @Size(max = 180) String name, @Size(max = 32) String phone,
			@Size(max = 500) String note, boolean marketingConsent) {
		CustomerService.CustomerInput toInput() { return new CustomerService.CustomerInput(name, phone, note, marketingConsent); }
	}

	public record CustomerResponse(UUID id, String name, String phone, String note, boolean marketingConsent,
			Instant consentUpdatedAt, boolean active, Instant anonymizedAt) {
		static CustomerResponse from(Customer customer) {
			return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(), customer.getNote(),
					customer.isMarketingConsent(), customer.getConsentUpdatedAt(), customer.isActive(), customer.getAnonymizedAt());
		}
	}
}
