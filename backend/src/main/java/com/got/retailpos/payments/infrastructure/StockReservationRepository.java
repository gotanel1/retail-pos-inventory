package com.got.retailpos.payments.infrastructure;
import java.time.Instant; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import com.got.retailpos.payments.domain.*; import jakarta.persistence.LockModeType;
public interface StockReservationRepository extends JpaRepository<StockReservation,UUID>{
	@EntityGraph(attributePaths="items") Optional<StockReservation> findBySaleId(UUID saleId);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="items") @Query("select reservation from StockReservation reservation where reservation.saleId=:saleId") Optional<StockReservation> findBySaleIdForUpdate(@Param("saleId") UUID saleId);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="items") @Query("select reservation from StockReservation reservation where reservation.id=:id") Optional<StockReservation> findByIdForUpdate(@Param("id") UUID id);
	@Query("select reservation.id from StockReservation reservation where reservation.status='ACTIVE' and reservation.expiresAt<=:now") List<UUID> findExpiredActiveIds(@Param("now") Instant now);
}
