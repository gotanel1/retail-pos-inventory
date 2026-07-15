package com.got.retailpos.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.got.retailpos.inventory.application.MovingAverageCostCalculator;

class MovingAverageCostCalculatorTests {

	private final MovingAverageCostCalculator calculator = new MovingAverageCostCalculator();

	@Test
	void shouldUseReceivedCostWhenCurrentStockIsZero() {
		assertThat(calculator.calculate(0, new BigDecimal("0.0000"), 5, new BigDecimal("12.3456")))
				.isEqualByComparingTo("12.3456");
	}

	@Test
	void shouldWeightCostByQuantities() {
		assertThat(calculator.calculate(10, new BigDecimal("100.0000"), 5, new BigDecimal("130.0000")))
				.isEqualByComparingTo("110.0000");
	}

	@Test
	void shouldRoundHalfUpToFourDecimalPlaces() {
		assertThat(calculator.calculate(2, new BigDecimal("1.0000"), 1, new BigDecimal("1.0001")))
				.isEqualByComparingTo("1.0000");
		assertThat(calculator.calculate(2, new BigDecimal("1.0000"), 1, new BigDecimal("1.0002")))
				.isEqualByComparingTo("1.0001");
	}

	@Test
	void shouldRejectInvalidQuantitiesAndCosts() {
		assertThatThrownBy(() -> calculator.calculate(0, BigDecimal.ZERO, 0, BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> calculator.calculate(0, BigDecimal.ZERO, 1, new BigDecimal("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
