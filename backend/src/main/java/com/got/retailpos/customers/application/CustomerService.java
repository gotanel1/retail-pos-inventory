package com.got.retailpos.customers.application;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.got.retailpos.customers.domain.Customer;
import com.got.retailpos.customers.infrastructure.CustomerRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class CustomerService implements CustomerReader {

	private final CustomerRepository repository;

	public CustomerService(CustomerRepository repository) { this.repository = repository; }

	@Transactional(readOnly = true)
	public Page<Customer> findAll(String search, Pageable pageable) {
		return StringUtils.hasText(search) ? repository.search(search.strip(), pageable) : repository.findAllByActiveTrue(pageable);
	}

	@Transactional(readOnly = true)
	public Customer findById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ลูกค้า"));
	}

	@Override @Transactional(readOnly = true)
	public boolean isActiveCustomer(UUID id) { return repository.findById(id).map(Customer::isActive).orElse(false); }

	@Transactional
	public Customer create(CustomerInput input) {
		var clean = clean(input);
		ensurePhoneAvailable(clean.normalizedPhone(), null);
		return repository.save(Customer.create(clean.name(), clean.phone(), clean.normalizedPhone(), clean.note(), input.marketingConsent()));
	}

	@Transactional
	public Customer update(UUID id, CustomerInput input) {
		var customer = findById(id);
		var clean = clean(input);
		ensurePhoneAvailable(clean.normalizedPhone(), id);
		customer.update(clean.name(), clean.phone(), clean.normalizedPhone(), clean.note(), input.marketingConsent());
		return customer;
	}

	@Transactional
	public void anonymize(UUID id) {
		findById(id).anonymize();
	}

	private CleanCustomer clean(CustomerInput input) {
		if (!StringUtils.hasText(input.name())) throw new IllegalArgumentException("ชื่อลูกค้าห้ามว่าง");
		var phone = StringUtils.hasText(input.phone()) ? input.phone().strip() : null;
		var normalized = phone == null ? null : phone.replaceAll("[^0-9+]", "");
		if (normalized != null && normalized.length() < 9) throw new IllegalArgumentException("เบอร์โทรไม่ถูกต้อง");
		return new CleanCustomer(input.name().strip(), phone, normalized, StringUtils.hasText(input.note()) ? input.note().strip() : null);
	}

	private void ensurePhoneAvailable(String phone, UUID currentId) {
		if (phone == null) return;
		var exists = currentId == null ? repository.existsByNormalizedPhone(phone) : repository.existsByNormalizedPhoneAndIdNot(phone, currentId);
		if (exists) throw new CustomerConflictException("เบอร์โทรนี้ถูกใช้กับลูกค้ารายอื่นแล้ว");
	}

	public record CustomerInput(String name, String phone, String note, boolean marketingConsent) {}
	private record CleanCustomer(String name, String phone, String normalizedPhone, String note) {}
}
