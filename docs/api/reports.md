# Reporting API

All reporting endpoints require an authenticated `OWNER` or `MANAGER` session. Dates are interpreted in the configured business time zone (`Asia/Bangkok` by default), and responses use completed sales only.

## Dashboard

```http
GET /api/v1/reports/dashboard?from=2026-07-01&to=2026-07-15
```

Both query parameters are optional and default to the current business date. The range is inclusive and must not exceed 366 days.

```json
{
  "from": "2026-07-01",
  "to": "2026-07-15",
  "summary": {
    "salesCount": 42,
    "totalSales": 12500.00,
    "netSalesExcludingVat": 11682.24,
    "costOfGoodsSold": 7100.00,
    "grossProfit": 4582.24,
    "inventoryValue": 38950.0000
  },
  "payments": [
    { "method": "CASH", "transactionCount": 32, "amount": 9200.00 },
    { "method": "PROMPTPAY", "transactionCount": 10, "amount": 3300.00 }
  ],
  "lowStock": [],
  "movements": []
}
```

`grossProfit` is net sales excluding VAT minus the cost snapshots stored on completed sale items. `inventoryValue` is the current on-hand quantity multiplied by the current moving-average cost. These are operational dashboard figures, not certified accounting statements.

Invalid date ranges return an `application/problem+json` response. The low-stock and movement collections are limited to 20 rows each.
