package com.got.retailpos.identity.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.identity.application.ManagerPinService;
import com.got.retailpos.identity.security.RetailUserPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/auth/manager-pin")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerPinController {
	private final ManagerPinService service;
	public ManagerPinController(ManagerPinService service) { this.service = service; }

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void configure(@Valid @RequestBody ConfigurePinRequest body, Authentication authentication) {
		service.configure(((RetailUserPrincipal) authentication.getPrincipal()).id(), body.currentPassword(), body.pin());
	}

	public record ConfigurePinRequest(@NotBlank String currentPassword, @Pattern(regexp = "^[0-9]{4,6}$") String pin) {}
}
