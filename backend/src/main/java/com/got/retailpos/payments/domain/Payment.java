package com.got.retailpos.payments.domain;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID; import jakarta.persistence.*;
@Entity @Table(name="payments")
public class Payment {
	@Id private UUID id; @Column(name="sale_id",nullable=false) private UUID saleId; @Enumerated(EnumType.STRING) @Column(name="payment_method",nullable=false,length=24) private PaymentMethod paymentMethod; @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private PaymentStatus status; @Column(nullable=false,precision=19,scale=2) private BigDecimal amount; @Column(name="provider_event_id",unique=true,length=255) private String providerEventId; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="completed_at") private Instant completedAt; protected Payment(){}
	public static Payment succeededCash(UUID saleId,BigDecimal amount){var p=new Payment();p.id=UUID.randomUUID();p.saleId=saleId;p.paymentMethod=PaymentMethod.CASH;p.status=PaymentStatus.SUCCEEDED;p.amount=amount;p.createdAt=Instant.now();p.completedAt=p.createdAt;return p;}
}
