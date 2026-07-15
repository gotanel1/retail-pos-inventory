package com.got.retailpos.sales.application;
import java.math.*; import org.springframework.stereotype.Component; import com.got.retailpos.sales.domain.DiscountType;
@Component public class DiscountCalculator {
	public BigDecimal calculate(BigDecimal subtotal,DiscountType type,BigDecimal value){if(value==null||value.signum()<0)throw new IllegalArgumentException("ส่วนลดต้องไม่ติดลบ"); BigDecimal amount=switch(type){case AMOUNT->value;case PERCENT->{if(value.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("ส่วนลดเปอร์เซ็นต์ต้องไม่เกิน 100");yield subtotal.multiply(value).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);}};amount=amount.setScale(2,RoundingMode.HALF_UP);if(amount.compareTo(subtotal)>0)throw new IllegalArgumentException("ส่วนลดมากกว่ายอดขาย");return amount;}
}
