package com.got.retailpos.payments.domain;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="stock_reservation_items")
public class StockReservationItem {
	@Id private UUID id;
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="reservation_id") private StockReservation reservation;
	@Column(name="product_id",nullable=false) private UUID productId;
	@Column(nullable=false) private int quantity;
	protected StockReservationItem(){}
	static StockReservationItem create(StockReservation reservation,UUID productId,int quantity){if(quantity<=0)throw new IllegalArgumentException("จำนวนจองต้องมากกว่าศูนย์");var item=new StockReservationItem();item.id=UUID.randomUUID();item.reservation=reservation;item.productId=productId;item.quantity=quantity;return item;}
	public UUID getProductId(){return productId;} public int getQuantity(){return quantity;}
}
