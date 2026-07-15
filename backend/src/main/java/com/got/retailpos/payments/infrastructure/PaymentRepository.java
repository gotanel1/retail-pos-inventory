package com.got.retailpos.payments.infrastructure;
import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import com.got.retailpos.payments.domain.Payment; import jakarta.persistence.LockModeType;
public interface PaymentRepository extends JpaRepository<Payment,UUID>{
	Optional<Payment> findBySaleId(UUID saleId);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select payment from Payment payment where payment.id=:id") Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select payment from Payment payment where payment.providerPaymentIntentId=:intentId") Optional<Payment> findByProviderPaymentIntentIdForUpdate(@Param("intentId") String intentId);
}
