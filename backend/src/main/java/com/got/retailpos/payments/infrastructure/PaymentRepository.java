package com.got.retailpos.payments.infrastructure;
import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository; import com.got.retailpos.payments.domain.Payment;
public interface PaymentRepository extends JpaRepository<Payment,UUID>{}
