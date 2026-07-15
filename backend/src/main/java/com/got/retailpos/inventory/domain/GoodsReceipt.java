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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "supplier_id", nullable = false)
	private Supplier supplier;

	@Column(name = "reference_number", unique = true, length = 100)
	private String referenceNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GoodsReceiptStatus status;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	@Column(length = 500)
	private String note;

	@Column(name = "received_by", nullable = false)
	private UUID receivedBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<GoodsReceiptItem> items = new ArrayList<>();

	protected GoodsReceipt() {
	}

	private GoodsReceipt(
			Supplier supplier,
			String referenceNumber,
			Instant receivedAt,
			String note,
			UUID receivedBy) {
		this.id = UUID.randomUUID();
		this.supplier = supplier;
		this.referenceNumber = referenceNumber;
		this.status = GoodsReceiptStatus.POSTED;
		this.receivedAt = receivedAt;
		this.note = note;
		this.receivedBy = receivedBy;
		this.createdAt = Instant.now();
	}

	public static GoodsReceipt post(
			Supplier supplier,
			String referenceNumber,
			Instant receivedAt,
			String note,
			UUID receivedBy) {
		return new GoodsReceipt(supplier, referenceNumber, receivedAt, note, receivedBy);
	}

	public void addItem(UUID productId, int quantity, java.math.BigDecimal unitCost) {
		items.add(GoodsReceiptItem.create(this, productId, quantity, unitCost));
	}

	public UUID getId() {
		return id;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public GoodsReceiptStatus getStatus() {
		return status;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

	public String getNote() {
		return note;
	}

	public UUID getReceivedBy() {
		return receivedBy;
	}

	public List<GoodsReceiptItem> getItems() {
		return Collections.unmodifiableList(items);
	}
}
