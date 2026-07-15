package com.got.retailpos.payments.application;

import java.math.BigDecimal; import java.time.*; import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.got.retailpos.inventory.domain.StockMovement; import com.got.retailpos.inventory.infrastructure.*; import com.got.retailpos.payments.domain.*; import com.got.retailpos.payments.infrastructure.*; import com.got.retailpos.sales.domain.*; import com.got.retailpos.sales.infrastructure.SaleRepository; import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class PromptPayTransactions {
	private final SaleRepository sales; private final PaymentRepository payments; private final StockReservationRepository reservations; private final InventoryBalanceRepository balances; private final StockMovementRepository movements; private final ReceiptNumberGenerator receipts; private final JdbcTemplate jdbc;
	public PromptPayTransactions(SaleRepository sales,PaymentRepository payments,StockReservationRepository reservations,InventoryBalanceRepository balances,StockMovementRepository movements,ReceiptNumberGenerator receipts,JdbcTemplate jdbc){this.sales=sales;this.payments=payments;this.reservations=reservations;this.balances=balances;this.movements=movements;this.receipts=receipts;this.jdbc=jdbc;}

	@Transactional public PendingContext reserve(UUID saleId,String key,UUID actor){
		if(key==null||key.isBlank()||key.length()>100)throw new IllegalArgumentException("ต้องระบุ Idempotency-Key ไม่เกิน 100 ตัวอักษร");
		var sale=sales.findByIdForUpdate(saleId).orElseThrow(()->new ResourceNotFoundException("บิลขาย"));
		if(sale.getStatus()==SaleStatus.AWAITING_PAYMENT){if(!key.equals(sale.getCheckoutIdempotencyKey()))throw new IllegalStateException("บิลกำลังรอชำระด้วย key อื่น");return existing(sale);}
		if(sale.getStatus()!=SaleStatus.DRAFT)throw new IllegalStateException("เริ่ม PromptPay ของบิลนี้ไม่ได้");
		var expiresAt=Instant.now().plus(Duration.ofMinutes(10)); var reservation=StockReservation.active(saleId,expiresAt,actor);
		for(var item:sorted(sale)){balances.ensureExists(item.getProductId());var balance=balances.findByProductIdForUpdate(item.getProductId()).orElseThrow();balance.reserve(item.getQuantity());reservation.addItem(item.getProductId(),item.getQuantity());}
		sale.awaitPayment(key);reservations.save(reservation);var payment=payments.save(Payment.pendingPromptPay(saleId,sale.getTotal(),expiresAt));
		return context(payment,reservation);
	}

	@Transactional public PendingContext attach(UUID paymentId,String intentId,String qr){var payment=payments.findByIdForUpdate(paymentId).orElseThrow(()->new ResourceNotFoundException("payment"));payment.attachProvider(intentId,qr);var reservation=reservations.findBySaleId(payment.getSaleId()).orElseThrow();return context(payment,reservation);}

	@Transactional public void process(String eventId,String eventType,String intentId,PromptPayGateway.ProviderStatus providerStatus){
		var inserted=jdbc.update("INSERT INTO stripe_events(event_id,event_type,payment_intent_id,received_at) VALUES (?,?,?,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",eventId,eventType,intentId);if(inserted==0)return;
		var payment=payments.findByProviderPaymentIntentIdForUpdate(intentId)
			.orElseThrow(()->new IllegalStateException("ยังไม่พบ PaymentIntent ในระบบ กรุณาให้ Stripe ส่ง webhook ซ้ำ"));
		if(providerStatus==PromptPayGateway.ProviderStatus.SUCCEEDED)complete(eventId,payment);
		else if(providerStatus==PromptPayGateway.ProviderStatus.FAILED||providerStatus==PromptPayGateway.ProviderStatus.CANCELLED)release(eventId,payment,false);
		markProcessed(eventId);
	}

	private void complete(String eventId,Payment payment){
		if(payment.getStatus()==PaymentStatus.SUCCEEDED)return;
		var sale=sales.findByIdForUpdate(payment.getSaleId()).orElseThrow();var reservation=reservations.findBySaleIdForUpdate(sale.getId()).orElseThrow();var active=reservation.getStatus()==ReservationStatus.ACTIVE;var stockMoves=new ArrayList<StockMovement>();var now=Instant.now();
		for(var reservationItem:reservation.getItems().stream().sorted(Comparator.comparing(i->i.getProductId().toString())).toList()){
			var balance=balances.findByProductIdForUpdate(reservationItem.getProductId()).orElseThrow();if(active)balance.consumeReservation(reservationItem.getQuantity());else balance.sell(reservationItem.getQuantity());
			var saleItem=sale.getItems().stream().filter(i->i.getProductId().equals(reservationItem.getProductId())).findFirst().orElseThrow();saleItem.snapshotCost(balance.getAverageCost());stockMoves.add(StockMovement.sale(reservationItem.getProductId(),reservationItem.getQuantity(),balance,sale.getId(),reservation.getCreatedBy(),now));
		}
		sales.flush();movements.saveAll(stockMoves);if(active)reservation.consume();payment.succeed(eventId);sale.completePromptPay(receipts.next(),reservation.getCreatedBy());
	}

	@Transactional public String expire(UUID reservationId){var reservation=reservations.findByIdForUpdate(reservationId).orElseThrow();if(reservation.getStatus()!=ReservationStatus.ACTIVE||reservation.getExpiresAt().isAfter(Instant.now()))return null;var payment=payments.findBySaleId(reservation.getSaleId()).orElseThrow();release(null,payment,true);return payment.getProviderPaymentIntentId();}

	private void release(String eventId,Payment payment,boolean expired){var reservation=reservations.findBySaleIdForUpdate(payment.getSaleId()).orElseThrow();if(reservation.getStatus()!=ReservationStatus.ACTIVE)return;var sale=sales.findByIdForUpdate(payment.getSaleId()).orElseThrow();for(var item:reservation.getItems().stream().sorted(Comparator.comparing(i->i.getProductId().toString())).toList()){var balance=balances.findByProductIdForUpdate(item.getProductId()).orElseThrow();balance.releaseReservation(item.getQuantity());}if(expired){reservation.expire();payment.cancel();}else{reservation.release();payment.fail(eventId);}sale.expirePayment();}
	private PendingContext existing(Sale sale){var payment=payments.findBySaleId(sale.getId()).orElseThrow();var reservation=reservations.findBySaleId(sale.getId()).orElseThrow();return context(payment,reservation);}
	private PendingContext context(Payment payment,StockReservation reservation){return new PendingContext(payment.getId(),payment.getSaleId(),payment.getAmount(),payment.getProviderPaymentIntentId(),payment.getQrCodeDataUrl(),reservation.getExpiresAt());}
	private List<SaleItem> sorted(Sale sale){return sale.getItems().stream().sorted(Comparator.comparing(i->i.getProductId().toString())).toList();}
	private void markProcessed(String eventId){jdbc.update("UPDATE stripe_events SET processed_at=CURRENT_TIMESTAMP WHERE event_id=?",eventId);}
	public record PendingContext(UUID paymentId,UUID saleId,BigDecimal amount,String paymentIntentId,String qrCodeImageUrl,Instant expiresAt){}
}
