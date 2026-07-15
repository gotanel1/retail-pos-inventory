package com.got.retailpos.payments.web;
import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import com.got.retailpos.payments.application.PromptPayService; import com.got.retailpos.payments.infrastructure.StripeWebhookVerifier;
@RestController @RequestMapping("/api/v1/payments/stripe/webhook") public class StripeWebhookController {
	private final StripeWebhookVerifier verifier; private final PromptPayService service;
	public StripeWebhookController(StripeWebhookVerifier verifier,PromptPayService service){this.verifier=verifier;this.service=service;}
	@PostMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void webhook(@RequestBody String payload,@RequestHeader("Stripe-Signature") String signature){var event=verifier.verify(payload,signature);if(event.eventType().startsWith("payment_intent."))service.processWebhook(event.eventId(),event.eventType(),event.paymentIntentId());}
}
