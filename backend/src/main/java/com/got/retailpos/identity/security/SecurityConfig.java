package com.got.retailpos.identity.security;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.got.retailpos.identity.infrastructure.UserAccountRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) throws Exception {
		http
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository)
						.requireExplicitSave(true))
				.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository).ignoringRequestMatchers("/api/v1/payments/stripe/webhook"))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET,
								"/", "/index.html", "/assets/**", "/favicon.ico",
								"/actuator/health/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
								"/api/v1/auth/csrf")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/payments/stripe/webhook").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeProblem(response, HttpStatus.UNAUTHORIZED,
										"ต้องเข้าสู่ระบบ", "กรุณาเข้าสู่ระบบก่อนใช้งานส่วนนี้"))
						.accessDeniedHandler((request, response, exception) ->
								writeProblem(response, HttpStatus.FORBIDDEN,
										"ไม่มีสิทธิ์ใช้งาน", "บัญชีนี้ไม่มีสิทธิ์ทำรายการที่ร้องขอ")))
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpStatus.NO_CONTENT.value())))
				.requestCache(cache -> cache.disable())
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives(
								"default-src 'self'; img-src 'self' data: https://*.stripe.com; "
										+ "style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; "
										+ "object-src 'none'; base-uri 'self'; frame-ancestors 'none'"))
						.referrerPolicy(referrer -> referrer
								.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
						.permissionsPolicyHeader(permissions -> permissions
								.policy("camera=(), microphone=(), geolocation=()")))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	UserDetailsService userDetailsService(UserAccountRepository repository) {
		return username -> repository.findByUsernameIgnoreCase(username)
				.map(RetailUserPrincipal::from)
				.orElseThrow(() -> new InvalidCredentialsException());
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		var provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(
				new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		return new HttpSessionCsrfTokenRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeSessionAuthenticationStrategy(List.of(
				new ChangeSessionIdAuthenticationStrategy(),
				new CsrfAuthenticationStrategy(csrfTokenRepository)));
	}

	private void writeProblem(
			HttpServletResponse response,
			HttpStatus status,
			String title,
			String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("""
				{"status":%d,"title":"%s","detail":"%s"}
				""".formatted(status.value(), title, detail).strip());
	}
}
