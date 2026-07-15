package com.got.retailpos.identity.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.infrastructure.UserAccountRepository;
import com.got.retailpos.shared.application.ResourceNotFoundException;

@Service
public class ManagerPinService {

	private static final int MAX_FAILURES = 5;
	private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final ConcurrentHashMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

	public ManagerPinService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void configure(UUID userId, String currentPassword, String pin) {
		if (pin == null || !pin.matches("^[0-9]{4,6}$")) throw new IllegalArgumentException("Manager PIN ต้องเป็นตัวเลข 4-6 หลัก");
		var user = repository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("ผู้ใช้"));
		if (user.getRole() != Role.OWNER && user.getRole() != Role.MANAGER) throw new IllegalArgumentException("บทบาทนี้ตั้ง Manager PIN ไม่ได้");
		if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) throw new IllegalArgumentException("รหัสผ่านปัจจุบันไม่ถูกต้อง");
		user.configureManagerPin(passwordEncoder.encode(pin));
	}

	@Transactional(readOnly = true)
	public UUID verify(String username, String pin) {
		var key = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
		var state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
		synchronized (state) {
			if (state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now())) throw new ManagerPinRateLimitException();
			var user = repository.findByUsernameIgnoreCase(key).orElse(null);
			var valid = user != null && user.isActive() && (user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER)
					&& user.getManagerPinHash() != null && passwordEncoder.matches(pin == null ? "" : pin, user.getManagerPinHash());
			if (valid) { attempts.remove(key); return user.getId(); }
			state.failures++;
			if (state.failures >= MAX_FAILURES) { state.lockedUntil = Instant.now().plus(LOCK_DURATION); throw new ManagerPinRateLimitException(); }
			throw new InvalidManagerPinException();
		}
	}

	private static final class AttemptState { private int failures; private Instant lockedUntil; }
}
