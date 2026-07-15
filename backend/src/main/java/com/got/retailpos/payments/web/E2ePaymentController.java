package com.got.retailpos.payments.web;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.got.retailpos.payments.application.PromptPayService;
import com.got.retailpos.payments.infrastructure.E2ePromptPayGateway;

@RestController
@Profile("e2e")
@RequestMapping("/api/v1/test/payments")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','CASHIER')")
public class E2ePaymentController {

	private final E2ePromptPayGateway gateway;
	private final PromptPayService payments;

	public E2ePaymentController(E2ePromptPayGateway gateway, PromptPayService payments) {
		this.gateway = gateway;
		this.payments = payments;
	}

	@PostMapping("/{paymentIntentId}/succeed")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void succeed(@PathVariable String paymentIntentId) {
		gateway.succeed(paymentIntentId);
		payments.processWebhook("evt_e2e_" + UUID.randomUUID(), "payment_intent.succeeded", paymentIntentId);
	}
}
