package com.got.retailpos.sales.infrastructure;
import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import com.got.retailpos.sales.domain.Sale; import jakarta.persistence.LockModeType;
public interface SaleRepository extends JpaRepository<Sale,UUID>{
	@EntityGraph(attributePaths="items") Optional<Sale> findOneById(UUID id);
	@EntityGraph(attributePaths="items") Optional<Sale> findByCheckoutIdempotencyKey(String key);
	@Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths="items") @Query("select sale from Sale sale where sale.id=:id") Optional<Sale> findByIdForUpdate(@Param("id") UUID id);
	Page<Sale> findAllBy(Pageable pageable);
}
