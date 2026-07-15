package com.got.retailpos.inventory.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.got.retailpos.catalog.application.ProductCatalogReader;
import com.got.retailpos.inventory.domain.InventoryCount;
import com.got.retailpos.inventory.domain.InventoryCountStatus;
import com.got.retailpos.inventory.domain.MovementType;
import com.got.retailpos.inventory.domain.StockMovement;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository;
import com.got.retailpos.inventory.infrastructure.InventoryCountRepository;
import com.got.retailpos.inventory.infrastructure.StockMovementRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class InventoryCountService {

	private final ProductCatalogReader catalogReader;
	private final InventoryBalanceRepository balanceRepository;
	private final InventoryCountRepository countRepository;
	private final StockMovementRepository movementRepository;

	public InventoryCountService(ProductCatalogReader catalogReader, InventoryBalanceRepository balanceRepository,
			InventoryCountRepository countRepository, StockMovementRepository movementRepository) {
		this.catalogReader = catalogReader;
		this.balanceRepository = balanceRepository;
		this.countRepository = countRepository;
		this.movementRepository = movementRepository;
	}

	@Transactional
	public InventoryCount submit(CountInput input, UUID actorId) {
		validate(input);
		var productIds = input.items().stream().map(CountItemInput::productId).toList();
		if (catalogReader.findActiveProducts(productIds).size() != productIds.size()) {
			throw new ResourceNotFoundException("สินค้าบางรายการ");
		}
		var count = InventoryCount.submit(input.reason().strip(), input.countedAt(), actorId);
		for (var item : input.items()) {
			balanceRepository.ensureExists(item.productId());
			var balance = balanceRepository.findById(item.productId()).orElseThrow();
			count.addItem(item.productId(), balance.getOnHand(), item.countedQuantity());
		}
		return countRepository.save(count);
	}

	@Transactional
	public InventoryCount approve(UUID id, UUID managerId) {
		var count = countRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Inventory Count"));
		if (count.getStatus() != InventoryCountStatus.SUBMITTED) throw new InventoryConflictException("Inventory Count นี้ถูกพิจารณาแล้ว");
		var movements = new java.util.ArrayList<StockMovement>();
		var now = Instant.now();
		for (var item : count.getItems().stream().sorted(Comparator.comparing(value -> value.getProductId().toString())).toList()) {
			balanceRepository.ensureExists(item.getProductId());
			var balance = balanceRepository.findByProductIdForUpdate(item.getProductId()).orElseThrow();
			if (balance.getOnHand() != item.getExpectedOnHand()) {
				throw new InventoryConflictException("ยอดสต็อกเปลี่ยนหลังการตรวจนับ กรุณาตรวจนับใหม่");
			}
			if (item.getDifference() != 0) {
				balance.adjust(item.getDifference());
				movements.add(StockMovement.adjustment(item.getProductId(), item.getDifference(), balance,
						count.getId(), count.getReason(), managerId, now));
			}
		}
		count.approve(managerId);
		movementRepository.saveAll(movements);
		return count;
	}

	@Transactional
	public InventoryCount reject(UUID id, UUID managerId) {
		var count = countRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Inventory Count"));
		if (count.getStatus() != InventoryCountStatus.SUBMITTED) throw new InventoryConflictException("Inventory Count นี้ถูกพิจารณาแล้ว");
		count.reject(managerId);
		return count;
	}

	@Transactional(readOnly = true)
	public InventoryCount findById(UUID id) {
		return countRepository.findOneById(id).orElseThrow(() -> new ResourceNotFoundException("Inventory Count"));
	}

	@Transactional(readOnly = true)
	public Page<InventoryCount> findAll(Pageable pageable) {
		var page = countRepository.findAllBy(pageable);
		page.forEach(count -> count.getItems().size());
		return page;
	}

	@Transactional(readOnly = true)
	public Page<StockMovement> findAdjustments(Pageable pageable) {
		return movementRepository.findAllByMovementTypeIn(List.of(MovementType.ADJUSTMENT_IN, MovementType.ADJUSTMENT_OUT), pageable);
	}

	public java.util.Map<UUID, ProductCatalogReader.CatalogProduct> findProducts(java.util.Collection<UUID> ids) {
		return catalogReader.findActiveProducts(ids);
	}

	private void validate(CountInput input) {
		if (!StringUtils.hasText(input.reason())) throw new IllegalArgumentException("ต้องระบุเหตุผลการตรวจนับ");
		if (input.countedAt() == null || input.countedAt().isAfter(Instant.now())) throw new IllegalArgumentException("เวลาตรวจนับต้องไม่อยู่ในอนาคต");
		if (input.items() == null || input.items().isEmpty() || input.items().size() > 200) throw new IllegalArgumentException("ต้องมีสินค้า 1-200 รายการ");
		var ids = new HashSet<UUID>();
		for (var item : input.items()) {
			if (item.productId() == null || !ids.add(item.productId())) throw new IllegalArgumentException("สินค้าใน Count ห้ามซ้ำกัน");
			if (item.countedQuantity() < 0) throw new IllegalArgumentException("ยอดนับต้องไม่ติดลบ");
		}
	}

	public record CountInput(String reason, Instant countedAt, List<CountItemInput> items) {}
	public record CountItemInput(UUID productId, int countedQuantity) {}
}
