package com.got.retailpos.payments.application;
import java.time.Instant; import org.slf4j.*; import org.springframework.scheduling.annotation.*; import org.springframework.stereotype.Component; import com.got.retailpos.payments.infrastructure.StockReservationRepository;
@Component @EnableScheduling public class ReservationExpiryJob {
	private static final Logger log=LoggerFactory.getLogger(ReservationExpiryJob.class); private final StockReservationRepository reservations; private final PromptPayTransactions transactions; private final PromptPayGateway gateway;
	public ReservationExpiryJob(StockReservationRepository reservations,PromptPayTransactions transactions,PromptPayGateway gateway){this.reservations=reservations;this.transactions=transactions;this.gateway=gateway;}
	@Scheduled(fixedDelayString="${app.reservation-expiry-interval-ms:30000}") public void expire(){for(var id:reservations.findExpiredActiveIds(Instant.now())){var intent=transactions.expire(id);if(intent!=null)try{gateway.cancel(intent);}catch(RuntimeException exception){log.error("ยกเลิก PaymentIntent {} หลัง reservation หมดอายุไม่สำเร็จ",intent,exception);}}}
}
