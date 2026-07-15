package com.got.retailpos.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

	@Id
	private UUID id;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 32)
	private MovementType movementType;

	@Column(name = "quantity_delta", nullable = false)
	private int quantityDelta;

	@Column(name = "on_hand_after", nullable = false)
	private int onHandAfter;

	@Column(name = "reserved_after", nullable = false)
	private int reservedAfter;

	@Column(name = "unit_cost", precision = 19, scale = 4)
	private BigDecimal unitCost;

	@Column(name = "average_cost_after", nullable = false, precision = 19, scale = 4)
	private BigDecimal averageCostAfter;

	@Column(name = "reference_type", nullable = false, length = 40)
	private String referenceType;

	@Column(name = "reference_id", nullable = false)
	private UUID referenceId;

	@Column(length = 500)
	private String reason;

	@Column(name = "actor_user_id", nullable = false)
	private UUID actorUserId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected StockMovement() {
	}

	private StockMovement(
			UUID productId,
			MovementType movementType,
			int quantityDelta,
			int onHandAfter,
			int reservedAfter,
			BigDecimal unitCost,
			BigDecimal averageCostAfter,
			String referenceType,
			UUID referenceId,
			String reason,
			UUID actorUserId,
			Instant occurredAt) {
		this.id = UUID.randomUUID();
		this.productId = productId;
		this.movementType = movementType;
		this.quantityDelta = quantityDelta;
		this.onHandAfter = onHandAfter;
		this.reservedAfter = reservedAfter;
		this.unitCost = unitCost;
		this.averageCostAfter = averageCostAfter;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.reason = reason;
		this.actorUserId = actorUserId;
		this.occurredAt = occurredAt;
	}

	public static StockMovement receive(
			UUID productId,
			int quantity,
			InventoryBalance balance,
			BigDecimal unitCost,
			UUID receiptId,
			String reason,
			UUID actorUserId,
			Instant occurredAt) {
		return new StockMovement(
				productId,
				MovementType.RECEIVE,
				quantity,
				balance.getOnHand(),
				balance.getReserved(),
				unitCost,
				balance.getAverageCost(),
				"GOODS_RECEIPT",
				receiptId,
				reason,
				actorUserId,
				occurredAt);
	}

	public static StockMovement adjustment(
			UUID productId,
			int quantityDelta,
			InventoryBalance balance,
			UUID countId,
			String reason,
			UUID actorUserId,
			Instant occurredAt) {
		if (quantityDelta == 0) throw new IllegalArgumentException("Adjustment ต้องเปลี่ยนยอดสต็อก");
		return new StockMovement(
				productId,
				quantityDelta > 0 ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT,
				quantityDelta,
				balance.getOnHand(),
				balance.getReserved(),
				null,
				balance.getAverageCost(),
				"INVENTORY_COUNT",
				countId,
				reason,
				actorUserId,
				occurredAt);
	}

	public static StockMovement sale(UUID productId, int quantity, InventoryBalance balance, UUID saleId,
			UUID actorUserId, Instant occurredAt) {
		return new StockMovement(productId, MovementType.SALE, -quantity, balance.getOnHand(), balance.getReserved(),
				balance.getAverageCost(), balance.getAverageCost(), "SALE", saleId, null, actorUserId, occurredAt);
	}

	public UUID getId() { return id; }
	public UUID getProductId() { return productId; }
	public MovementType getMovementType() { return movementType; }
	public int getQuantityDelta() { return quantityDelta; }
	public int getOnHandAfter() { return onHandAfter; }
	public int getReservedAfter() { return reservedAfter; }
	public BigDecimal getUnitCost() { return unitCost; }
	public BigDecimal getAverageCostAfter() { return averageCostAfter; }
	public String getReferenceType() { return referenceType; }
	public UUID getReferenceId() { return referenceId; }
	public String getReason() { return reason; }
	public UUID getActorUserId() { return actorUserId; }
	public Instant getOccurredAt() { return occurredAt; }
}
