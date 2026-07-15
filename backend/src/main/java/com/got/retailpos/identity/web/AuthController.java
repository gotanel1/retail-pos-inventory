package com.got.retailpos.identity.web;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.identity.domain.Role;
import com.got.retailpos.identity.security.InvalidCredentialsException;
import com.got.retailpos.identity.security.RetailUserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository securityContextRepository;
	private final SecurityContextHolderStrategy securityContextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	public AuthController(
			AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository securityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/login")
	public CurrentUserResponse login(
			@Valid @RequestBody LoginRequest body,
			HttpServletRequest request,
			HttpServletResponse response) {
		try {
			var authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
			sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

			var context = securityContextHolderStrategy.createEmptyContext();
			context.setAuthentication(authentication);
			securityContextHolderStrategy.setContext(context);
			securityContextRepository.saveContext(context, request, response);
			return CurrentUserResponse.from(authentication);
		}
		catch (AuthenticationException exception) {
			throw new InvalidCredentialsException();
		}
	}

	@GetMapping("/me")
	public CurrentUserResponse me(Authentication authentication) {
		return CurrentUserResponse.from(authentication);
	}

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getToken());
	}

	public record LoginRequest(
			@NotBlank @Size(max = 80) String username,
			@NotBlank @Size(max = 200) String password) {
	}

	public record CsrfResponse(String headerName, String token) {
	}

	public record CurrentUserResponse(UUID id, String username, String displayName, Role role) {

		static CurrentUserResponse from(Authentication authentication) {
			var principal = (RetailUserPrincipal) authentication.getPrincipal();
			return new CurrentUserResponse(
					principal.id(), principal.getUsername(), principal.displayName(), principal.role());
		}
	}
}
