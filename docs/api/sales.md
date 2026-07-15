# Sales and Cash Checkout API

Sales endpoints require an authenticated `OWNER`, `MANAGER`, or `CASHIER` session. Mutating requests require the CSRF header. Monetary values use two decimal places; the server is the source of truth for price, discount, VAT, and totals.

## Create a draft sale

```http
POST /api/v1/sales HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{
  "customerId": null,
  "items": [
    { "productId": "6d8bc4de-7d63-4ed8-b3e5-d244e351334c", "quantity": 2 }
  ]
}
```

The response is a `DRAFT` sale with immutable product identity, name, unit-price, and VAT-setting snapshots. A customer is optional but must be active when supplied.

## Apply a manager-approved discount

```http
POST /api/v1/sales/{saleId}/discount HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{
  "type": "PERCENT",
  "value": 10.00,
  "managerUsername": "manager",
  "managerPin": "1234"
}
```

`type` is `AMOUNT` or `PERCENT`. The approver must be an active `OWNER` or `MANAGER`. The response includes `discountApprovedBy`; the PIN is never persisted in plain text or returned.

## Complete a cash checkout

```http
POST /api/v1/sales/{saleId}/checkout/cash HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token
Idempotency-Key: 50f9870c-fd07-4918-891e-a5ff16cce61d

{ "cashReceived": 500.00 }
```

Checkout atomically locks inventory balances, prevents negative available stock, records average-cost snapshots, appends immutable `SALE` movements, stores the successful cash payment, and returns a `COMPLETED` sale with receipt number and change. Retrying the same operation with the same idempotency key returns the original completed sale.

Conflicting sale state or reused idempotency data returns `application/problem+json`. Completed sales and their items are protected from update and deletion by PostgreSQL triggers.

## Store settings and Manager PIN

- `GET /api/v1/store-settings`: authenticated roles
- `PUT /api/v1/store-settings`: `OWNER` only
- `POST /api/v1/auth/manager-pin`: `OWNER` or `MANAGER`, requires the user's current password

Store VAT settings are copied into new sales. Updating settings never changes historical receipts.
