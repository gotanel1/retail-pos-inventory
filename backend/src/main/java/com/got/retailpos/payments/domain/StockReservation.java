package com.got.retailpos.payments.domain;

import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;

@Entity @Table(name="stock_reservations")
public class StockReservation {
	@Id private UUID id;
	@Column(name="sale_id",nullable=false,unique=true) private UUID saleId;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private ReservationStatus status;
	@Column(name="expires_at",nullable=false) private Instant expiresAt;
	@Column(name="created_by",nullable=false) private UUID createdBy;
	@Column(name="created_at",nullable=false) private Instant createdAt;
	@Column(name="closed_at") private Instant closedAt;
	@Version private long version;
	@OneToMany(mappedBy="reservation",cascade=CascadeType.ALL,fetch=FetchType.LAZY) private List<StockReservationItem> items=new ArrayList<>();
	protected StockReservation(){}
	public static StockReservation active(UUID saleId,Instant expiresAt,UUID actor){var r=new StockReservation();r.id=UUID.randomUUID();r.saleId=saleId;r.status=ReservationStatus.ACTIVE;r.expiresAt=expiresAt;r.createdBy=actor;r.createdAt=Instant.now();return r;}
	public void addItem(UUID productId,int quantity){if(status!=ReservationStatus.ACTIVE)throw new IllegalStateException("เพิ่มรายการจองไม่ได้");items.add(StockReservationItem.create(this,productId,quantity));}
	public void consume(){close(ReservationStatus.CONSUMED);} public void release(){close(ReservationStatus.RELEASED);} public void expire(){close(ReservationStatus.EXPIRED);}
	private void close(ReservationStatus target){if(status!=ReservationStatus.ACTIVE)throw new IllegalStateException("ปิด reservation ซ้ำไม่ได้");status=target;closedAt=Instant.now();}
	public UUID getId(){return id;} public UUID getSaleId(){return saleId;} public ReservationStatus getStatus(){return status;} public Instant getExpiresAt(){return expiresAt;} public UUID getCreatedBy(){return createdBy;} public List<StockReservationItem> getItems(){return Collections.unmodifiableList(items);}
}
