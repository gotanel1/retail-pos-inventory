package com.got.retailpos.inventory.application;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.got.retailpos.inventory.domain.Supplier;
import com.got.retailpos.inventory.infrastructure.SupplierRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class SupplierService {

	private final SupplierRepository repository;

	public SupplierService(SupplierRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<Supplier> findAll() {
		return repository.findAllByOrderByNameAsc();
	}

	@Transactional
	public Supplier create(SupplierInput input) {
		var name = cleanRequired(input.name(), "ชื่อ Supplier");
		var normalizedName = normalize(name);
		if (repository.existsByNormalizedName(normalizedName)) {
			throw new InventoryConflictException("มี Supplier ชื่อนี้แล้ว: " + name);
		}
		return repository.save(Supplier.create(
				name,
				normalizedName,
				cleanOptional(input.phone()),
				cleanOptional(input.note())));
	}

	@Transactional
	public Supplier update(UUID id, SupplierInput input) {
		var supplier = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier"));
		var name = cleanRequired(input.name(), "ชื่อ Supplier");
		var normalizedName = normalize(name);
		if (repository.existsByNormalizedNameAndIdNot(normalizedName, id)) {
			throw new InventoryConflictException("มี Supplier ชื่อนี้แล้ว: " + name);
		}
		supplier.update(name, normalizedName, cleanOptional(input.phone()), cleanOptional(input.note()));
		return supplier;
	}

	private String normalize(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
	}

	private String cleanRequired(String value, String label) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(label + " ห้ามว่าง");
		}
		return value.strip();
	}

	private String cleanOptional(String value) {
		return StringUtils.hasText(value) ? value.strip() : null;
	}

	public record SupplierInput(String name, String phone, String note) {
	}
}
