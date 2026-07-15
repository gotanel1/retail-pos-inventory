# Stock Count and Customer API

Inventory staff may submit counts with `POST /api/v1/inventory/counts`. Each item snapshots the current system quantity. Only `OWNER` and `MANAGER` may call `/inventory/counts/{id}/approve` or `/reject`.

Approval locks each balance and fails with `409 Conflict` if stock changed after submission. Successful differences append `ADJUSTMENT_IN` or `ADJUSTMENT_OUT` movements. Adjustment history is available at `GET /api/v1/inventory/adjustments`.

Customer endpoints are available to `OWNER`, `MANAGER`, and `CASHIER` at `/api/v1/customers`. Phone numbers are normalized for duplicate detection. `DELETE /customers/{id}` is restricted to owner/manager and anonymizes identifying fields instead of deleting the row. This preserves future sale-history relationships without claiming legal PDPA certification.
