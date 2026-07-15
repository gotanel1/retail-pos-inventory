package com.got.retailpos.inventory.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
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
import com.got.retailpos.inventory.domain.GoodsReceipt;
import com.got.retailpos.inventory.domain.StockMovement;
import com.got.retailpos.inventory.infrastructure.GoodsReceiptRepository;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository;
import com.got.retailpos.inventory.infrastructure.InventoryBalanceRepository.BalanceView;
import com.got.retailpos.inventory.infrastructure.StockMovementRepository;
import com.got.retailpos.inventory.infrastructure.SupplierRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class InventoryService {

	private static final int MAX_RECEIPT_ITEMS = 200;

	private final ProductCatalogReader catalogReader;
	private final SupplierRepository supplierRepository;
	private final InventoryBalanceRepository balanceRepository;
	private final GoodsReceiptRepository receiptRepository;
	private final StockMovementRepository movementRepository;
	private final MovingAverageCostCalculator costCalculator;

	public InventoryService(
			ProductCatalogReader catalogReader,
			SupplierRepository supplierRepository,
			InventoryBalanceRepository balanceRepository,
			GoodsReceiptRepository receiptRepository,
			StockMovementRepository movementRepository,
			MovingAverageCostCalculator costCalculator) {
		this.catalogReader = catalogReader;
		this.supplierRepository = supplierRepository;
		this.balanceRepository = balanceRepository;
		this.receiptRepository = receiptRepository;
		this.movementRepository = movementRepository;
		this.costCalculator = costCalculator;
	}

	@Transactional(readOnly = true)
	public Page<BalanceView> findBalances(String search, boolean lowStock, Pageable pageable) {
		return balanceRepository.findBalanceViews(StringUtils.hasText(search) ? search.strip() : "", lowStock, pageable);
	}

	@Transactional(readOnly = true)
	public Page<StockMovement> findMovements(UUID productId, Pageable pageable) {
		return productId == null
				? movementRepository.findAll(pageable)
				: movementRepository.findAllByProductId(productId, pageable);
	}

	@Transactional(readOnly = true)
	public Page<GoodsReceipt> findReceipts(Pageable pageable) {
		return receiptRepository.findAllBy(pageable);
	}

	@Transactional(readOnly = true)
	public GoodsReceipt findReceipt(UUID id) {
		return receiptRepository.findOneById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Goods Receipt"));
	}

	@Transactional
	public GoodsReceipt postReceipt(ReceiptInput input, UUID actorUserId) {
		validateReceipt(input);
		var supplier = supplierRepository.findById(input.supplierId())
				.filter(supplierRecord -> supplierRecord.isActive())
				.orElseThrow(() -> new ResourceNotFoundException("Supplier"));
		var referenceNumber = cleanOptional(input.referenceNumber());
		if (referenceNumber != null && receiptRepository.existsByReferenceNumber(referenceNumber)) {
			throw new InventoryConflictException("เลขอ้างอิง Goods Receipt ถูกใช้แล้ว: " + referenceNumber);
		}

		var productIds = input.items().stream().map(ReceiptItemInput::productId).toList();
		var products = catalogReader.findActiveProducts(productIds);
		if (products.size() != productIds.size()) {
			throw new ResourceNotFoundException("สินค้าบางรายการ");
		}

		var receipt = GoodsReceipt.post(
				supplier,
				referenceNumber,
				input.receivedAt(),
				cleanOptional(input.note()),
				actorUserId);
		var occurredAt = Instant.now();
		var sortedItems = input.items().stream()
				.sorted(Comparator.comparing(item -> item.productId().toString()))
				.toList();
		var movements = new java.util.ArrayList<StockMovement>();

		for (var item : sortedItems) {
			var unitCost = normalizeCost(item.unitCost());
			balanceRepository.ensureExists(item.productId());
			var balance = balanceRepository.findByProductIdForUpdate(item.productId())
					.orElseThrow(() -> new ResourceNotFoundException("ยอดคงเหลือสินค้า"));
			var newAverageCost = costCalculator.calculate(
					balance.getOnHand(),
					balance.getAverageCost(),
					item.quantity(),
					unitCost);
			balance.receive(item.quantity(), newAverageCost);
			receipt.addItem(item.productId(), item.quantity(), unitCost);
			movements.add(StockMovement.receive(
					item.productId(),
					item.quantity(),
					balance,
					unitCost,
					receipt.getId(),
					cleanOptional(input.note()),
					actorUserId,
					occurredAt));
		}

		receiptRepository.save(receipt);
		movementRepository.saveAll(movements);
		return receipt;
	}

	public java.util.Map<UUID, ProductCatalogReader.CatalogProduct> findProducts(Collection<UUID> ids) {
		return catalogReader.findActiveProducts(ids);
	}

	private void validateReceipt(ReceiptInput input) {
		if (input.receivedAt() == null || input.receivedAt().isAfter(Instant.now())) {
			throw new IllegalArgumentException("เวลารับสินค้าต้องไม่อยู่ในอนาคต");
		}
		if (input.items() == null || input.items().isEmpty()) {
			throw new IllegalArgumentException("Goods Receipt ต้องมีสินค้าอย่างน้อยหนึ่งรายการ");
		}
		if (input.items().size() > MAX_RECEIPT_ITEMS) {
			throw new IllegalArgumentException("Goods Receipt มีสินค้าได้ไม่เกิน 200 รายการ");
		}
		var uniqueProductIds = new HashSet<UUID>();
		for (var item : input.items()) {
			if (item.productId() == null || !uniqueProductIds.add(item.productId())) {
				throw new IllegalArgumentException("สินค้าใน Goods Receipt ห้ามซ้ำกัน");
			}
			if (item.quantity() <= 0) {
				throw new IllegalArgumentException("จำนวนรับเข้าต้องมากกว่าศูนย์");
			}
			normalizeCost(item.unitCost());
		}
	}

	private BigDecimal normalizeCost(BigDecimal cost) {
		if (cost == null || cost.signum() < 0 || cost.scale() > 4) {
			throw new IllegalArgumentException("ต้นทุนต้องไม่ติดลบและมีทศนิยมไม่เกิน 4 ตำแหน่ง");
		}
		return cost.setScale(4, RoundingMode.UNNECESSARY);
	}

	private String cleanOptional(String value) {
		return StringUtils.hasText(value) ? value.strip() : null;
	}

	public record ReceiptInput(
			UUID supplierId,
			String referenceNumber,
			Instant receivedAt,
			String note,
			List<ReceiptItemInput> items) {
	}

	public record ReceiptItemInput(UUID productId, int quantity, BigDecimal unitCost) {
	}
}
