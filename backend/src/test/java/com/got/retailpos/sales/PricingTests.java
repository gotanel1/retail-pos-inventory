package com.got.retailpos.sales;
import static org.assertj.core.api.Assertions.*; import java.math.BigDecimal; import java.util.UUID; import org.junit.jupiter.api.Test; import com.got.retailpos.sales.application.DiscountCalculator; import com.got.retailpos.sales.domain.*;
class PricingTests {
	private final DiscountCalculator calculator=new DiscountCalculator();
	@Test void shouldCalculateAmountAndPercentDiscount(){assertThat(calculator.calculate(new BigDecimal("100.00"),DiscountType.AMOUNT,new BigDecimal("12.34"))).isEqualByComparingTo("12.34");assertThat(calculator.calculate(new BigDecimal("199.00"),DiscountType.PERCENT,new BigDecimal("10"))).isEqualByComparingTo("19.90");}
	@Test void shouldRejectExcessiveDiscount(){assertThatThrownBy(()->calculator.calculate(new BigDecimal("100"),DiscountType.PERCENT,new BigDecimal("101"))).isInstanceOf(IllegalArgumentException.class);}
	@Test void shouldExtractVatFromDiscountedInclusivePrice(){var sale=Sale.draft(null,true,new BigDecimal("7.00"),UUID.randomUUID());sale.addItem(UUID.randomUUID(),"SKU","สินค้า",1,new BigDecimal("107.00"));sale.applyDiscount(DiscountType.AMOUNT,new BigDecimal("7.00"),new BigDecimal("7.00"),UUID.randomUUID());assertThat(sale.getTotal()).isEqualByComparingTo("100.00");assertThat(sale.getVatAmount()).isEqualByComparingTo("6.54");}
}
