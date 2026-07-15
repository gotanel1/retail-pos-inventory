package com.got.retailpos.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "goods_receipt_items")
public class GoodsReceiptItem {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "receipt_id", nullable = false)
	private GoodsReceipt receipt;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
	private BigDecimal unitCost;

	protected GoodsReceiptItem() {
	}

	private GoodsReceiptItem(GoodsReceipt receipt, UUID productId, int quantity, BigDecimal unitCost) {
		this.id = UUID.randomUUID();
		this.receipt = receipt;
		this.productId = productId;
		this.quantity = quantity;
		this.unitCost = unitCost;
	}

	static GoodsReceiptItem create(GoodsReceipt receipt, UUID productId, int quantity, BigDecimal unitCost) {
		return new GoodsReceiptItem(receipt, productId, quantity, unitCost);
	}

	public UUID getProductId() {
		return productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitCost() {
		return unitCost;
	}
}
