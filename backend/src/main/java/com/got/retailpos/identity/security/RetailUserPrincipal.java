package com.got.retailpos.identity.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.domain.UserAccount;

public final class RetailUserPrincipal implements UserDetails {

	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID id;
	private final String username;
	private final String password;
	private final String displayName;
	private final Role role;
	private final boolean active;

	private RetailUserPrincipal(UserAccount account) {
		this.id = account.getId();
		this.username = account.getUsername();
		this.password = account.getPasswordHash();
		this.displayName = account.getDisplayName();
		this.role = account.getRole();
		this.active = account.isActive();
	}

	public static RetailUserPrincipal from(UserAccount account) {
		return new RetailUserPrincipal(account);
	}

	public UUID id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public Role role() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
