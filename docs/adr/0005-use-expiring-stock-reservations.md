# ADR 0005: Use Expiring Stock Reservations for Asynchronous Payments

- Status: Accepted
- Date: 2026-07-15

## Context

Cash checkout can confirm payment and deduct stock in one database transaction. PromptPay is asynchronous: Stripe creates a QR code first and a signed webhook confirms the final result later. Deducting stock when the QR is created would produce false sales when customers abandon payment, while leaving stock fully available could oversell the last item before payment completes.

## Decision

Reserve the requested quantity for ten minutes by increasing `reserved` while leaving `on_hand` unchanged. `available` remains `on_hand - reserved`.

The workflow uses two short database transactions around the external Stripe call:

1. Lock the sale and inventory balances, create a pending payment and active reservation, then commit.
2. Create the Stripe PaymentIntent outside a database transaction.
3. Attach the PaymentIntent ID and QR URL in a second short transaction.
4. On a signed webhook, retrieve the current PaymentIntent state from Stripe and atomically consume or release the reservation.

Stripe event IDs are stored with a primary-key constraint to make processing idempotent. If a webhook arrives before the PaymentIntent ID has been attached, the transaction rolls back so Stripe can retry instead of permanently losing the event.

An expiry job releases active reservations after ten minutes and attempts to cancel the corresponding PaymentIntent. A late successful payment may complete only if stock can be reacquired atomically; otherwise processing fails and remains available for reconciliation.

## Consequences

- Pending PromptPay payments temporarily reduce sellable stock without changing physical stock.
- Browser state and QR display never determine payment success; only verified provider state can complete a sale.
- External API calls do not hold database row locks open.
- Expiry and webhook processing can race safely because both lock the payment, reservation, sale, and balances.
- Late success without available stock requires operational reconciliation rather than silently creating negative stock.
