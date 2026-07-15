package com.got.retailpos.inventory.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_counts")
public class InventoryCount {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InventoryCountStatus status;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "counted_at", nullable = false)
	private Instant countedAt;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "approved_by")
	private UUID approvedBy;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@OneToMany(mappedBy = "inventoryCount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<InventoryCountItem> items = new ArrayList<>();

	protected InventoryCount() {
	}

	private InventoryCount(String reason, Instant countedAt, UUID createdBy) {
		this.id = UUID.randomUUID();
		this.status = InventoryCountStatus.SUBMITTED;
		this.reason = reason;
		this.countedAt = countedAt;
		this.createdBy = createdBy;
		this.createdAt = Instant.now();
	}

	public static InventoryCount submit(String reason, Instant countedAt, UUID createdBy) {
		return new InventoryCount(reason, countedAt, createdBy);
	}

	public void addItem(UUID productId, int expectedOnHand, int countedQuantity) {
		items.add(InventoryCountItem.create(this, productId, expectedOnHand, countedQuantity));
	}

	public void approve(UUID managerId) {
		complete(InventoryCountStatus.APPROVED, managerId);
	}

	public void reject(UUID managerId) {
		complete(InventoryCountStatus.REJECTED, managerId);
	}

	private void complete(InventoryCountStatus newStatus, UUID managerId) {
		if (status != InventoryCountStatus.SUBMITTED) {
			throw new IllegalStateException("Inventory Count นี้ถูกพิจารณาแล้ว");
		}
		status = newStatus;
		approvedBy = managerId;
		approvedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public InventoryCountStatus getStatus() { return status; }
	public String getReason() { return reason; }
	public Instant getCountedAt() { return countedAt; }
	public UUID getCreatedBy() { return createdBy; }
	public Instant getCreatedAt() { return createdAt; }
	public UUID getApprovedBy() { return approvedBy; }
	public Instant getApprovedAt() { return approvedAt; }
	public List<InventoryCountItem> getItems() { return Collections.unmodifiableList(items); }
}
