package com.got.retailpos.identity.application;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.domain.UserAccount;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;

@Service
public class UserAccountService {

	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserAccount> findAll() {
		return repository.findAll().stream()
				.sorted((left, right) -> left.getUsername().compareTo(right.getUsername()))
				.toList();
	}

	@Transactional
	public UserAccount create(String username, String rawPassword, String displayName, Role role) {
		var normalizedUsername = normalizeUsername(username);
		if (repository.existsByUsernameIgnoreCase(normalizedUsername)) {
			throw new UsernameAlreadyExistsException(normalizedUsername);
		}

		var account = UserAccount.create(
				normalizedUsername,
				passwordEncoder.encode(rawPassword),
				displayName.strip(),
				role);
		return repository.save(account);
	}

	@Transactional(readOnly = true)
	public boolean hasAnyUser() {
		return repository.count() > 0;
	}

	private String normalizeUsername(String username) {
		return username.strip().toLowerCase(Locale.ROOT);
	}
}
