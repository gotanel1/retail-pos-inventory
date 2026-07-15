package com.got.retailpos.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "normalized_name", nullable = false, unique = true, length = 120)
	private String normalizedName;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Category() {
	}

	private Category(String name, String normalizedName) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.name = name;
		this.normalizedName = normalizedName;
		this.active = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Category create(String name, String normalizedName) {
		return new Category(name, normalizedName);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public boolean isActive() {
		return active;
	}
}
