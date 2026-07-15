package com.got.retailpos.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(nullable = false, unique = true, length = 80)
	private String sku;

	@Column(unique = true, length = 64)
	private String barcode;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "low_stock_threshold", nullable = false)
	private int lowStockThreshold;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	private Product(
			Category category,
			String sku,
			String barcode,
			String name,
			BigDecimal salePrice,
			int lowStockThreshold) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.category = category;
		this.sku = sku;
		this.barcode = barcode;
		this.name = name;
		this.salePrice = salePrice;
		this.lowStockThreshold = lowStockThreshold;
		this.active = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Product create(
			Category category,
			String sku,
			String barcode,
			String name,
			BigDecimal salePrice,
			int lowStockThreshold) {
		return new Product(category, sku, barcode, name, salePrice, lowStockThreshold);
	}

	public void update(
			Category category,
			String sku,
			String barcode,
			String name,
			BigDecimal salePrice,
			int lowStockThreshold) {
		this.category = category;
		this.sku = sku;
		this.barcode = barcode;
		this.name = name;
		this.salePrice = salePrice;
		this.lowStockThreshold = lowStockThreshold;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public String getSku() {
		return sku;
	}

	public String getBarcode() {
		return barcode;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public int getLowStockThreshold() {
		return lowStockThreshold;
	}

	public boolean isActive() {
		return active;
	}
}
