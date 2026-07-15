package com.got.retailpos.inventory.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class Supplier {

	@Id
	private UUID id;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(name = "normalized_name", nullable = false, unique = true, length = 180)
	private String normalizedName;

	@Column(length = 32)
	private String phone;

	@Column(length = 500)
	private String note;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Supplier() {
	}

	private Supplier(String name, String normalizedName, String phone, String note) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.name = name;
		this.normalizedName = normalizedName;
		this.phone = phone;
		this.note = note;
		this.active = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Supplier create(String name, String normalizedName, String phone, String note) {
		return new Supplier(name, normalizedName, phone, note);
	}

	public void update(String name, String normalizedName, String phone, String note) {
		this.name = name;
		this.normalizedName = normalizedName;
		this.phone = phone;
		this.note = note;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}

	public String getNote() {
		return note;
	}

	public boolean isActive() {
		return active;
	}
}
