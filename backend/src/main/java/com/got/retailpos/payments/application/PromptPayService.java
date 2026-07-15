package com.got.retailpos.payments.application;
import java.util.UUID; import org.springframework.stereotype.Service;
@Service public class PromptPayService {
	private final PromptPayTransactions transactions; private final PromptPayGateway gateway;
	public PromptPayService(PromptPayTransactions transactions,PromptPayGateway gateway){this.transactions=transactions;this.gateway=gateway;}
	public PromptPayTransactions.PendingContext initiate(UUID saleId,String key,UUID actor){var pending=transactions.reserve(saleId,key,actor);if(pending.paymentIntentId()!=null)return pending;var created=gateway.create(pending.amount(),pending.saleId(),pending.paymentId(),key);return transactions.attach(pending.paymentId(),created.paymentIntentId(),created.qrCodeImageUrl());}
	public void processWebhook(String eventId,String eventType,String intentId){var status=gateway.retrieveStatus(intentId);transactions.process(eventId,eventType,intentId,status);}
}
