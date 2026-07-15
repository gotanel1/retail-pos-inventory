package com.got.retailpos.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.got.retailpos.identity.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCase(String username);
}
