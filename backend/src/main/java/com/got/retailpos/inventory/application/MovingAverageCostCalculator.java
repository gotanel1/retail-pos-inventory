package com.got.retailpos.inventory.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class MovingAverageCostCalculator {

	private static final int COST_SCALE = 4;

	public BigDecimal calculate(
			int currentQuantity,
			BigDecimal currentAverageCost,
			int receivedQuantity,
			BigDecimal receivedUnitCost) {
		if (currentQuantity < 0 || receivedQuantity <= 0) {
			throw new IllegalArgumentException("จำนวนสินค้าสำหรับคำนวณต้นทุนไม่ถูกต้อง");
		}
		if (currentAverageCost.signum() < 0 || receivedUnitCost.signum() < 0) {
			throw new IllegalArgumentException("ต้นทุนต้องไม่ติดลบ");
		}

		var currentValue = currentAverageCost.multiply(BigDecimal.valueOf(currentQuantity));
		var receivedValue = receivedUnitCost.multiply(BigDecimal.valueOf(receivedQuantity));
		return currentValue.add(receivedValue)
				.divide(BigDecimal.valueOf(Math.addExact(currentQuantity, receivedQuantity)), COST_SCALE, RoundingMode.HALF_UP);
	}
}
