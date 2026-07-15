package com.got.retailpos.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="payments")
public class Payment {
	@Id private UUID id;
	@Column(name="sale_id",nullable=false) private UUID saleId;
	@Enumerated(EnumType.STRING) @Column(name="payment_method",nullable=false,length=24) private PaymentMethod paymentMethod;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private PaymentStatus status;
	@Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
	@Column(name="provider_event_id",unique=true,length=255) private String providerEventId;
	@Column(name="provider_payment_intent_id",unique=true,length=255) private String providerPaymentIntentId;
	@Column(name="qr_code_data_url",columnDefinition="TEXT") private String qrCodeDataUrl;
	@Column(name="created_at",nullable=false) private Instant createdAt;
	@Column(name="updated_at",nullable=false) private Instant updatedAt;
	@Column(name="expires_at") private Instant expiresAt;
	@Column(name="completed_at") private Instant completedAt;
	protected Payment(){}
	public static Payment succeededCash(UUID saleId,BigDecimal amount){var p=new Payment();p.id=UUID.randomUUID();p.saleId=saleId;p.paymentMethod=PaymentMethod.CASH;p.status=PaymentStatus.SUCCEEDED;p.amount=amount;p.createdAt=Instant.now();p.updatedAt=p.createdAt;p.completedAt=p.createdAt;return p;}
	public static Payment pendingPromptPay(UUID saleId,BigDecimal amount,Instant expiresAt){var p=new Payment();p.id=UUID.randomUUID();p.saleId=saleId;p.paymentMethod=PaymentMethod.PROMPTPAY;p.status=PaymentStatus.PENDING;p.amount=amount;p.expiresAt=expiresAt;p.createdAt=Instant.now();p.updatedAt=p.createdAt;return p;}
	public void attachProvider(String intentId,String qrCode){if(status!=PaymentStatus.PENDING)throw new IllegalStateException("ผูก PaymentIntent ไม่ได้");if(providerPaymentIntentId!=null&&!providerPaymentIntentId.equals(intentId))throw new IllegalStateException("PaymentIntent ไม่ตรงกับรายการเดิม");providerPaymentIntentId=intentId;qrCodeDataUrl=qrCode;updatedAt=Instant.now();}
	public void succeed(String eventId){if(status==PaymentStatus.SUCCEEDED)return;if(paymentMethod!=PaymentMethod.PROMPTPAY)throw new IllegalStateException("ยืนยัน payment นี้ไม่ได้");status=PaymentStatus.SUCCEEDED;providerEventId=eventId;completedAt=Instant.now();updatedAt=completedAt;}
	public void fail(String eventId){if(status!=PaymentStatus.PENDING)return;status=PaymentStatus.FAILED;providerEventId=eventId;completedAt=Instant.now();updatedAt=completedAt;}
	public void cancel(){if(status!=PaymentStatus.PENDING)return;status=PaymentStatus.CANCELLED;completedAt=Instant.now();updatedAt=completedAt;}
	public UUID getId(){return id;} public UUID getSaleId(){return saleId;} public PaymentStatus getStatus(){return status;} public BigDecimal getAmount(){return amount;} public String getProviderPaymentIntentId(){return providerPaymentIntentId;} public String getQrCodeDataUrl(){return qrCodeDataUrl;} public Instant getExpiresAt(){return expiresAt;}
}
