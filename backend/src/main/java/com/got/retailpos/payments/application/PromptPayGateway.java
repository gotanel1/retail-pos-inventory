package com.got.retailpos.payments.application;
import java.math.BigDecimal; import java.util.UUID;
public interface PromptPayGateway {
	CreatedPayment create(BigDecimal amount,UUID saleId,UUID paymentId,String idempotencyKey);
	ProviderStatus retrieveStatus(String paymentIntentId);
	void cancel(String paymentIntentId);
	record CreatedPayment(String paymentIntentId,String qrCodeImageUrl,ProviderStatus status){}
	enum ProviderStatus { PENDING, SUCCEEDED, FAILED, CANCELLED }
}
