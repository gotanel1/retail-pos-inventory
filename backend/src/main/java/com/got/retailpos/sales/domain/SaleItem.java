package com.got.retailpos.sales.domain;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name="sale_items")
public class SaleItem {
	@Id private UUID id;
	@ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="sale_id") private Sale sale;
	@Column(name="product_id",nullable=false) private UUID productId;
	@Column(name="sku_snapshot",nullable=false,length=80) private String skuSnapshot;
	@Column(name="name_snapshot",nullable=false,length=180) private String nameSnapshot;
	@Column(nullable=false) private int quantity;
	@Column(name="unit_price",nullable=false,precision=19,scale=2) private BigDecimal unitPrice;
	@Column(name="unit_cost_snapshot",precision=19,scale=4) private BigDecimal unitCostSnapshot;
	@Column(name="line_total",nullable=false,precision=19,scale=2) private BigDecimal lineTotal;
	protected SaleItem() {}
	static SaleItem create(Sale sale, UUID productId, String sku, String name, int quantity, BigDecimal price) { var item=new SaleItem(); item.id=UUID.randomUUID(); item.sale=sale; item.productId=productId; item.skuSnapshot=sku; item.nameSnapshot=name; item.quantity=quantity; item.unitPrice=price; item.lineTotal=price.multiply(BigDecimal.valueOf(quantity)); return item; }
	public void snapshotCost(BigDecimal cost) { if (unitCostSnapshot != null) throw new IllegalStateException("บันทึกต้นทุนแล้ว"); unitCostSnapshot=cost; }
	public UUID getProductId(){return productId;} public String getSkuSnapshot(){return skuSnapshot;} public String getNameSnapshot(){return nameSnapshot;} public int getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;} public BigDecimal getUnitCostSnapshot(){return unitCostSnapshot;} public BigDecimal getLineTotal(){return lineTotal;}
}
