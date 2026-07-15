package com.got.retailpos.payments.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.got.retailpos.payments.application.PromptPayGateway;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;

@Component
@ConditionalOnProperty(name="app.payment.fake",havingValue="false",matchIfMissing=true)
public class StripePromptPayGateway implements PromptPayGateway {
	private final String secretKey;
	public StripePromptPayGateway(@Value("${app.stripe.secret-key:}") String secretKey){this.secretKey=secretKey;}
	@Override public CreatedPayment create(BigDecimal amount,UUID saleId,UUID paymentId,String idempotencyKey){
		try{
			var client=client();
			var method=PaymentIntentCreateParams.PaymentMethodData.builder().setType(PaymentIntentCreateParams.PaymentMethodData.Type.PROMPTPAY).build();
			var params=PaymentIntentCreateParams.builder().setAmount(amount.movePointRight(2).longValueExact()).setCurrency("thb").addPaymentMethodType("promptpay").setPaymentMethodData(method).setConfirm(true).putMetadata("sale_id",saleId.toString()).putMetadata("payment_id",paymentId.toString()).build();
			var options=RequestOptions.builder().setIdempotencyKey("promptpay-"+idempotencyKey).build();
			var intent=client.v1().paymentIntents().create(params,options);
			var qr=intent.getNextAction()!=null&&intent.getNextAction().getPromptpayDisplayQrCode()!=null?intent.getNextAction().getPromptpayDisplayQrCode().getImageUrlPng():null;
			return new CreatedPayment(intent.getId(),qr,map(intent.getStatus()));
		}catch(StripeException|ArithmeticException exception){throw new IllegalStateException("สร้าง Stripe PromptPay ไม่สำเร็จ",exception);}
	}
	@Override public ProviderStatus retrieveStatus(String paymentIntentId){try{return map(client().v1().paymentIntents().retrieve(paymentIntentId).getStatus());}catch(StripeException exception){throw new IllegalStateException("ตรวจสถานะ Stripe ไม่สำเร็จ",exception);}}
	@Override public void cancel(String paymentIntentId){try{client().v1().paymentIntents().cancel(paymentIntentId);}catch(StripeException exception){throw new IllegalStateException("ยกเลิก Stripe PaymentIntent ไม่สำเร็จ",exception);}}
	private StripeClient client(){if(secretKey==null||!secretKey.startsWith("sk_test_"))throw new IllegalStateException("ต้องตั้ง STRIPE_SECRET_KEY แบบ Test Mode");return new StripeClient(secretKey);}
	private ProviderStatus map(String status){return switch(status){case "succeeded"->ProviderStatus.SUCCEEDED;case "canceled"->ProviderStatus.CANCELLED;case "requires_payment_method"->ProviderStatus.FAILED;default->ProviderStatus.PENDING;};}
}
