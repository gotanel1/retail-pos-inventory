package com.got.retailpos.identity.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.identity.application.UserAccountService;
import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.domain.UserAccount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserAccountService service;

	public UserController(UserAccountService service) {
		this.service = service;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
	public List<UserResponse> findAll() {
		return service.findAll().stream().map(UserResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('OWNER')")
	public UserResponse create(@Valid @RequestBody CreateUserRequest body) {
		return UserResponse.from(service.create(body.username(), body.password(), body.displayName(), body.role()));
	}

	public record CreateUserRequest(
			@NotBlank @Size(min = 3, max = 80)
			@Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,
			@NotBlank @Size(min = 10, max = 200) String password,
			@NotBlank @Size(max = 120) String displayName,
			@NotNull Role role) {
	}

	public record UserResponse(UUID id, String username, String displayName, Role role, boolean active) {

		static UserResponse from(UserAccount account) {
			return new UserResponse(
					account.getId(), account.getUsername(), account.getDisplayName(), account.getRole(), account.isActive());
		}
	}
}
