package com.got.retailpos.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory_balances")
public class InventoryBalance {

	@Id
	@Column(name = "product_id")
	private UUID productId;

	@Column(name = "on_hand", nullable = false)
	private int onHand;

	@Column(nullable = false)
	private int reserved;

	@Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
	private BigDecimal averageCost;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected InventoryBalance() {
	}

	public void receive(int quantity, BigDecimal newAverageCost) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("จำนวนรับเข้าต้องมากกว่าศูนย์");
		}
		this.onHand = Math.addExact(this.onHand, quantity);
		this.averageCost = newAverageCost;
		this.updatedAt = Instant.now();
	}

	public void adjust(int quantityDelta) {
		var newOnHand = Math.addExact(onHand, quantityDelta);
		if (newOnHand < 0 || newOnHand < reserved) {
			throw new IllegalArgumentException("Adjustment ทำให้สต็อกติดลบหรือต่ำกว่ายอดจอง");
		}
		onHand = newOnHand;
		updatedAt = Instant.now();
	}

	public void sell(int quantity) {
		if (quantity <= 0) throw new IllegalArgumentException("จำนวนขายต้องมากกว่าศูนย์");
		if (getAvailable() < quantity) throw new IllegalArgumentException("สต็อกพร้อมขายไม่เพียงพอ");
		onHand = Math.subtractExact(onHand, quantity);
		updatedAt = Instant.now();
	}

	public UUID getProductId() {
		return productId;
	}

	public int getOnHand() {
		return onHand;
	}

	public int getReserved() {
		return reserved;
	}

	public int getAvailable() {
		return onHand - reserved;
	}

	public BigDecimal getAverageCost() {
		return averageCost;
	}
}
