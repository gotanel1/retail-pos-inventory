package com.got.retailpos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;

@Entity @Table(name="sales")
public class Sale {
	@Id private UUID id;
	@Column(name="receipt_number",unique=true,length=40) private String receiptNumber;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private SaleStatus status;
	@Column(name="customer_id") private UUID customerId;
	@Column(nullable=false,precision=19,scale=2) private BigDecimal subtotal;
	@Enumerated(EnumType.STRING) @Column(name="discount_type",length=16) private DiscountType discountType;
	@Column(name="discount_value",precision=19,scale=2) private BigDecimal discountValue;
	@Column(name="discount_amount",nullable=false,precision=19,scale=2) private BigDecimal discountAmount;
	@Column(name="vat_enabled",nullable=false) private boolean vatEnabled;
	@Column(name="vat_rate",nullable=false,precision=5,scale=2) private BigDecimal vatRate;
	@Column(name="vat_amount",nullable=false,precision=19,scale=2) private BigDecimal vatAmount;
	@Column(nullable=false,precision=19,scale=2) private BigDecimal total;
	@Column(name="cash_received",precision=19,scale=2) private BigDecimal cashReceived;
	@Column(name="change_amount",precision=19,scale=2) private BigDecimal changeAmount;
	@Column(name="checkout_idempotency_key",unique=true,length=100) private String checkoutIdempotencyKey;
	@Column(name="discount_approved_by") private UUID discountApprovedBy;
	@Column(name="created_by",nullable=false) private UUID createdBy;
	@Column(name="completed_by") private UUID completedBy;
	@Column(name="created_at",nullable=false) private Instant createdAt;
	@Column(name="updated_at",nullable=false) private Instant updatedAt;
	@Column(name="completed_at") private Instant completedAt;
	@Version private long version;
	@OneToMany(mappedBy="sale",cascade=CascadeType.ALL,fetch=FetchType.LAZY) private List<SaleItem> items=new ArrayList<>();
	protected Sale() {}
	public static Sale draft(UUID customerId, boolean vatEnabled, BigDecimal vatRate, UUID actor) { var s=new Sale(); s.id=UUID.randomUUID(); s.status=SaleStatus.DRAFT; s.customerId=customerId; s.subtotal=money(BigDecimal.ZERO); s.discountAmount=money(BigDecimal.ZERO); s.vatEnabled=vatEnabled; s.vatRate=vatRate; s.vatAmount=money(BigDecimal.ZERO); s.total=money(BigDecimal.ZERO); s.createdBy=actor; s.createdAt=Instant.now(); s.updatedAt=s.createdAt; return s; }
	public void addItem(UUID productId,String sku,String name,int quantity,BigDecimal price){if(status!=SaleStatus.DRAFT)throw new IllegalStateException("แก้บิลนี้ไม่ได้"); if(quantity<=0)throw new IllegalArgumentException("จำนวนขายต้องมากกว่าศูนย์"); items.add(SaleItem.create(this,productId,sku,name,quantity,price)); subtotal=money(subtotal.add(price.multiply(BigDecimal.valueOf(quantity)))); recalculate();}
	public void applyDiscount(DiscountType type,BigDecimal value,BigDecimal amount,UUID managerId){if(status!=SaleStatus.DRAFT)throw new IllegalStateException("แก้ส่วนลดไม่ได้");discountType=type;discountValue=value;discountAmount=amount;discountApprovedBy=managerId;recalculate();updatedAt=Instant.now();}
	public void complete(String receipt,String key,BigDecimal received,UUID actor){if(status!=SaleStatus.DRAFT)throw new IllegalStateException("Checkout บิลนี้ไม่ได้");status=SaleStatus.COMPLETED;receiptNumber=receipt;checkoutIdempotencyKey=key;cashReceived=received;changeAmount=money(received.subtract(total));completedBy=actor;completedAt=Instant.now();updatedAt=completedAt;}
	private void recalculate(){total=money(subtotal.subtract(discountAmount)); if(total.signum()<0)throw new IllegalArgumentException("ส่วนลดมากกว่ายอดขาย"); vatAmount=vatEnabled&&vatRate.signum()>0?money(total.subtract(total.divide(BigDecimal.ONE.add(vatRate.movePointLeft(2)),8,java.math.RoundingMode.HALF_UP))):money(BigDecimal.ZERO);}
	private static BigDecimal money(BigDecimal value){return value.setScale(2,java.math.RoundingMode.HALF_UP);}
	public UUID getId(){return id;} public String getReceiptNumber(){return receiptNumber;} public SaleStatus getStatus(){return status;} public UUID getCustomerId(){return customerId;} public BigDecimal getSubtotal(){return subtotal;} public DiscountType getDiscountType(){return discountType;} public BigDecimal getDiscountValue(){return discountValue;} public BigDecimal getDiscountAmount(){return discountAmount;} public boolean isVatEnabled(){return vatEnabled;} public BigDecimal getVatRate(){return vatRate;} public BigDecimal getVatAmount(){return vatAmount;} public BigDecimal getTotal(){return total;} public BigDecimal getCashReceived(){return cashReceived;} public BigDecimal getChangeAmount(){return changeAmount;} public String getCheckoutIdempotencyKey(){return checkoutIdempotencyKey;} public UUID getDiscountApprovedBy(){return discountApprovedBy;} public UUID getCreatedBy(){return createdBy;} public UUID getCompletedBy(){return completedBy;} public Instant getCreatedAt(){return createdAt;} public Instant getCompletedAt(){return completedAt;} public List<SaleItem> getItems(){return Collections.unmodifiableList(items);}
}
