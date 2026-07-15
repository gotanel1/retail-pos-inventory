package com.got.retailpos.sales.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_settings")
public class StoreSettings {
	@Id private Short id;
	@Column(name = "store_name", nullable = false, length = 180) private String storeName;
	@Column(name = "vat_enabled", nullable = false) private boolean vatEnabled;
	@Column(name = "vat_rate", nullable = false, precision = 5, scale = 2) private BigDecimal vatRate;
	@Column(name = "receipt_footer", length = 500) private String receiptFooter;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected StoreSettings() {}
	public void update(String name, boolean enabled, BigDecimal rate, String footer) { storeName = name; vatEnabled = enabled; vatRate = rate; receiptFooter = footer; updatedAt = Instant.now(); }
	public String getStoreName() { return storeName; }
	public boolean isVatEnabled() { return vatEnabled; }
	public BigDecimal getVatRate() { return vatRate; }
	public String getReceiptFooter() { return receiptFooter; }
}
