# PromptPay Payment API

All endpoints use `/api/v1`. Authenticated requests require the session cookie and CSRF header. Errors use `application/problem+json`.

## Start PromptPay checkout

```http
POST /api/v1/sales/{saleId}/checkout/promptpay
Idempotency-Key: 8ad415ec-9b49-4ce9-a105-82ffb30ecf33
Cookie: SESSION=...
X-CSRF-TOKEN: ...
```

The authenticated user must have `OWNER`, `MANAGER`, or `CASHIER`. The sale must be `DRAFT`, and enough `available` stock must exist. A successful response reserves stock for ten minutes:

```json
{
  "paymentId": "f5ff5fe0-04fb-437b-98e6-2521882fc5fb",
  "saleId": "4cc15114-330e-46cb-b13a-6e97f12e5ce5",
  "amount": 107.00,
  "paymentIntentId": "pi_...",
  "qrCodeImageUrl": "https://...",
  "expiresAt": "2026-07-15T11:10:00Z",
  "status": "PENDING"
}
```

Retry the same sale with the same idempotency key to retrieve the existing payment. A different key is rejected while the sale is awaiting payment.

## Poll sale status

```http
GET /api/v1/sales/{saleId}
```

The client may poll until the sale becomes `COMPLETED` or `EXPIRED`. The client must never infer success from displaying the QR code.

## Stripe webhook

```http
POST /api/v1/payments/stripe/webhook
Stripe-Signature: t=...,v1=...
Content-Type: application/json
```

This endpoint intentionally does not require a user session or CSRF token. It verifies the Stripe signature against the raw payload and `STRIPE_WEBHOOK_SECRET`, deduplicates by event ID, retrieves the current PaymentIntent state, and returns `204 No Content` after successful processing.

For local testing, configure `STRIPE_SECRET_KEY=sk_test_...`, start the application, and forward Stripe CLI events:

```powershell
stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook
```

Copy the displayed `whsec_...` value into `STRIPE_WEBHOOK_SECRET`, then restart the application. Never commit either secret.
