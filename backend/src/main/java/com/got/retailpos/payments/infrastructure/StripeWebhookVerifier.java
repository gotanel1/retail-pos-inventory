package com.got.retailpos.payments.infrastructure;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component; import com.google.gson.JsonParser; import com.stripe.net.Webhook;
@Component public class StripeWebhookVerifier {
	private final String secret;
	public StripeWebhookVerifier(@Value("${app.stripe.webhook-secret:}") String secret){this.secret=secret;}
	public VerifiedEvent verify(String payload,String signature){try{if(secret==null||!secret.startsWith("whsec_"))throw new IllegalStateException("ยังไม่ได้ตั้ง STRIPE_WEBHOOK_SECRET");var event=Webhook.constructEvent(payload,signature,secret);var intentId=JsonParser.parseString(payload).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("object").get("id").getAsString();if(intentId==null||intentId.isBlank())throw new IllegalArgumentException("Stripe event ไม่มี PaymentIntent ID");return new VerifiedEvent(event.getId(),event.getType(),intentId);}catch(Exception exception){throw new IllegalArgumentException("Stripe webhook signature หรือ payload ไม่ถูกต้อง",exception);}}
	public record VerifiedEvent(String eventId,String eventType,String paymentIntentId){}
}
