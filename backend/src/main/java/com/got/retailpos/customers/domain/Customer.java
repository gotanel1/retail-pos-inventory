package com.got.retailpos.customers.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

	@Id
	private UUID id;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(length = 32)
	private String phone;

	@Column(name = "normalized_phone", unique = true, length = 32)
	private String normalizedPhone;

	@Column(length = 500)
	private String note;

	@Column(name = "marketing_consent", nullable = false)
	private boolean marketingConsent;

	@Column(name = "consent_updated_at", nullable = false)
	private Instant consentUpdatedAt;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "anonymized_at")
	private Instant anonymizedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Customer() {
	}

	private Customer(String name, String phone, String normalizedPhone, String note, boolean marketingConsent) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.name = name;
		this.phone = phone;
		this.normalizedPhone = normalizedPhone;
		this.note = note;
		this.marketingConsent = marketingConsent;
		this.consentUpdatedAt = now;
		this.active = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Customer create(String name, String phone, String normalizedPhone, String note, boolean consent) {
		return new Customer(name, phone, normalizedPhone, note, consent);
	}

	public void update(String name, String phone, String normalizedPhone, String note, boolean consent) {
		if (!active) throw new IllegalStateException("ข้อมูลลูกค้านี้ถูก anonymize แล้ว");
		var now = Instant.now();
		if (marketingConsent != consent) consentUpdatedAt = now;
		this.name = name;
		this.phone = phone;
		this.normalizedPhone = normalizedPhone;
		this.note = note;
		this.marketingConsent = consent;
		this.updatedAt = now;
	}

	public void anonymize() {
		if (!active) return;
		var now = Instant.now();
		name = "ลูกค้าที่ลบ-" + id.toString().substring(0, 8);
		phone = null;
		normalizedPhone = null;
		note = null;
		marketingConsent = false;
		consentUpdatedAt = now;
		active = false;
		anonymizedAt = now;
		updatedAt = now;
	}

	public UUID getId() { return id; }
	public String getName() { return name; }
	public String getPhone() { return phone; }
	public String getNote() { return note; }
	public boolean isMarketingConsent() { return marketingConsent; }
	public Instant getConsentUpdatedAt() { return consentUpdatedAt; }
	public boolean isActive() { return active; }
	public Instant getAnonymizedAt() { return anonymizedAt; }
}
