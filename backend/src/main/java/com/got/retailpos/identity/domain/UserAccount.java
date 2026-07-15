package com.got.retailpos.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class UserAccount {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Role role;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserAccount() {
	}

	private UserAccount(String username, String passwordHash, String displayName, Role role) {
		var now = Instant.now();
		this.id = UUID.randomUUID();
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
		this.active = true;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static UserAccount create(String username, String passwordHash, String displayName, Role role) {
		return new UserAccount(username, passwordHash, displayName, role);
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Role getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}
}
