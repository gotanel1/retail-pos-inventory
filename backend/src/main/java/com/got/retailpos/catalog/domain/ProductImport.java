package com.got.retailpos.catalog.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_imports")
public class ProductImport {

	@Id
	private UUID id;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "raw_content", nullable = false, columnDefinition = "text")
	private String rawContent;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private ProductImportStatus status;

	@Column(name = "total_rows", nullable = false)
	private int totalRows;

	@Column(name = "valid_rows", nullable = false)
	private int validRows;

	@Column(name = "invalid_rows", nullable = false)
	private int invalidRows;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected ProductImport() {
	}

	private ProductImport(
			String originalFilename,
			String rawContent,
			int totalRows,
			int validRows,
			int invalidRows,
			UUID createdBy) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.originalFilename = originalFilename;
		this.rawContent = rawContent;
		this.status = ProductImportStatus.PREVIEWED;
		this.totalRows = totalRows;
		this.validRows = validRows;
		this.invalidRows = invalidRows;
		this.createdBy = createdBy;
		this.createdAt = now;
		this.expiresAt = now.plus(1, ChronoUnit.HOURS);
	}

	public static ProductImport preview(
			String originalFilename,
			String rawContent,
			int totalRows,
			int validRows,
			int invalidRows,
			UUID createdBy) {
		return new ProductImport(
				originalFilename, rawContent, totalRows, validRows, invalidRows, createdBy);
	}

	public void complete() {
		this.status = ProductImportStatus.COMPLETED;
		this.completedAt = Instant.now();
	}

	public boolean isExpired() {
		return !expiresAt.isAfter(Instant.now());
	}

	public UUID getId() {
		return id;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getRawContent() {
		return rawContent;
	}

	public ProductImportStatus getStatus() {
		return status;
	}

	public int getTotalRows() {
		return totalRows;
	}

	public int getValidRows() {
		return validRows;
	}

	public int getInvalidRows() {
		return invalidRows;
	}

	public UUID getCreatedBy() {
		return createdBy;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
