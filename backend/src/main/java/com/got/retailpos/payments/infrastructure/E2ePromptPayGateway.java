package com.got.retailpos.payments.infrastructure;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.got.retailpos.payments.application.PromptPayGateway;

@Component
@Profile("e2e")
@ConditionalOnProperty(name = "app.payment.fake", havingValue = "true")
public class E2ePromptPayGateway implements PromptPayGateway {

	private final Map<String, ProviderStatus> statuses = new ConcurrentHashMap<>();

	@Override
	public CreatedPayment create(BigDecimal amount, UUID saleId, UUID paymentId, String idempotencyKey) {
		var intentId = "pi_e2e_" + paymentId;
		statuses.putIfAbsent(intentId, ProviderStatus.PENDING);
		return new CreatedPayment(intentId, qrDataUrl(amount), statuses.get(intentId));
	}

	@Override
	public ProviderStatus retrieveStatus(String paymentIntentId) {
		return statuses.getOrDefault(paymentIntentId, ProviderStatus.PENDING);
	}

	@Override
	public void cancel(String paymentIntentId) {
		statuses.put(paymentIntentId, ProviderStatus.CANCELLED);
	}

	public void succeed(String paymentIntentId) {
		statuses.put(paymentIntentId, ProviderStatus.SUCCEEDED);
	}

	private String qrDataUrl(BigDecimal amount) {
		var svg = """
				<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240">
				  <rect width="240" height="240" fill="white"/>
				  <rect x="20" y="20" width="60" height="60" fill="#111"/>
				  <rect x="160" y="20" width="60" height="60" fill="#111"/>
				  <rect x="20" y="160" width="60" height="60" fill="#111"/>
				  <text x="120" y="125" text-anchor="middle" font-family="sans-serif" font-size="16">E2E %s THB</text>
				</svg>
				""".formatted(amount);
		return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
	}
}
