package com.got.retailpos.inventory.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_count_items")
public class InventoryCountItem {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "count_id", nullable = false)
	private InventoryCount inventoryCount;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "expected_on_hand", nullable = false)
	private int expectedOnHand;

	@Column(name = "counted_quantity", nullable = false)
	private int countedQuantity;

	@Column(nullable = false)
	private int difference;

	protected InventoryCountItem() {
	}

	private InventoryCountItem(InventoryCount inventoryCount, UUID productId, int expectedOnHand, int countedQuantity) {
		this.id = UUID.randomUUID();
		this.inventoryCount = inventoryCount;
		this.productId = productId;
		this.expectedOnHand = expectedOnHand;
		this.countedQuantity = countedQuantity;
		this.difference = Math.subtractExact(countedQuantity, expectedOnHand);
	}

	static InventoryCountItem create(InventoryCount count, UUID productId, int expectedOnHand, int countedQuantity) {
		return new InventoryCountItem(count, productId, expectedOnHand, countedQuantity);
	}

	public UUID getProductId() { return productId; }
	public int getExpectedOnHand() { return expectedOnHand; }
	public int getCountedQuantity() { return countedQuantity; }
	public int getDifference() { return difference; }
}
