# Inventory API

All endpoints require an authenticated session. `OWNER`, `MANAGER`, and `INVENTORY_STAFF` may create suppliers and post goods receipts. Read endpoints are available to every authenticated role.

## Post a goods receipt

```http
POST /api/v1/goods-receipts HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: generated-token

{
  "supplierId": "4bbf7777-a95d-4d12-a3a9-15c51dfdc2e2",
  "referenceNumber": "GR-2026-0001",
  "receivedAt": "2026-07-15T09:00:00Z",
  "note": "Delivery note 1001",
  "items": [
    {
      "productId": "6d8bc4de-7d63-4ed8-b3e5-d244e351334c",
      "quantity": 10,
      "unitCost": 100.0000
    }
  ]
}
```

Posting is atomic and immediately returns a `POSTED` receipt. The operation locks each product balance, updates moving-average cost, and appends one `RECEIVE` movement per item. Posted receipts cannot be edited or deleted.

## Read balances

```http
GET /api/v1/inventory/balances?search=hammer&lowStock=false&page=0&size=20&sort=sku,asc
```

Each result contains `onHand`, `reserved`, `available`, `averageCost`, and the product's low-stock threshold. Products without movements are returned with zero balances.

## Read movement history

```http
GET /api/v1/inventory/movements?productId={productId}&page=0&size=20&sort=occurredAt,desc
```

Movements include the quantity delta, resulting balance, cost snapshot, actor, timestamp, and originating business reference. Corrections must append a compensating adjustment; historical movements are immutable.

## Suppliers

Use `GET /api/v1/suppliers`, `POST /api/v1/suppliers`, and `PUT /api/v1/suppliers/{id}`. Supplier names are normalized and unique. Validation and conflicts use `application/problem+json`.
